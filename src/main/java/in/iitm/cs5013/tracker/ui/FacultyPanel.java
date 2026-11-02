package in.iitm.cs5013.tracker.ui;

import in.iitm.cs5013.tracker.service.*;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.*;

/** Faculty-facing management; all disk/network work uses the parent's worker. */
public final class FacultyPanel extends JPanel {
    @FunctionalInterface public interface Work { Object run() throws Exception; }
    @FunctionalInterface public interface Runner { void run(Work job,Consumer<Object> success); }
    private final TrackerPanel.Services services;
    private final Runner runner;
    private final Runnable refresh;
    private final Consumer<String> message;
    private final BiFunction<String,Runnable,JButton> button;
    private final SettingsService settings;
    private final RecordService records;
    private final JComboBox<RecordService.Kind> kind=new JComboBox<>(RecordService.Kind.values());
    private final JComboBox<String> recordId=new JComboBox<>();
    private final JTextField server=new JTextField(ClassroomService.DEFAULT_URL),link=new JTextField(),email=new JTextField();
    private final JComboBox<String> day=new JComboBox<>(new String[]{"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"});
    private final JTextField time=new JTextField("09:00",8);
    private final JSpinner threshold;
    private Properties preferences=new Properties();
    private String projectId;

    public FacultyPanel(TrackerPanel.Services services,Runner runner,Runnable refresh,Consumer<String> message,
                        BiFunction<String,Runnable,JButton> button,JSpinner threshold) {
        super(new BorderLayout());this.services=services;this.runner=runner;this.refresh=refresh;this.message=message;this.button=button;this.threshold=threshold;
        settings=new SettingsService(services.backups().database().path().getParent());
        records=new RecordService(services.backups().database(),services.clock());
        try {preferences=settings.load();threshold.setValue(settings.staleDays());server.setText(preferences.getProperty("formServer",ClassroomService.DEFAULT_URL));link.setText(preferences.getProperty("submissionUrl",""));}
        catch(Exception exception){message.accept("Preferences could not be loaded: "+exception.getMessage());}
        var tabs=new JTabbedPane();tabs.setName("faculty-tabs");tabs.addTab("Edit / delete",recordsPanel());tabs.addTab("Export / reminders",exportPanel());tabs.addTab("Settings / online updates",settingsPanel());add(tabs);
        kind.addActionListener(event->loadRecords());
    }
    public int savedThreshold(){return (Integer)threshold.getValue();}
    public void showProject(String id){projectId=id;email.setText(preferences.getProperty("email."+Objects.toString(id,""),""));loadRecords();}
    private JPanel recordsPanel() {
        JPanel panel=vertical();panel.add(new JLabel("Choose a record in the selected project. IDs cannot be changed."));panel.add(kind);panel.add(recordId);
        panel.add(button.apply("Edit selected record",()-> {
            var selected=selected();if(selected==null)return;
            var type=(RecordService.Kind)kind.getSelectedItem();
            runner.run(()->records.values(type,selected),result->{
                @SuppressWarnings("unchecked") var values=(List<String>)result;
                var fields=new ArrayList<JTextField>();var form=new JPanel(new GridLayout(0,2,8,8));
                for(int i=0;i<type.columns.length;i++){form.add(new JLabel(type.columns[i].replace('_',' ')));var field=new JTextField(values.get(i),32);fields.add(field);form.add(field);}
                if(JOptionPane.showConfirmDialog(this,form,"Edit "+selected+" (dates: yyyy-MM-dd; status: ACTIVE or COMPLETED)",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                    var edited=fields.stream().map(JTextField::getText).toList();
                    runner.run(()->{records.edit(type,selected,edited);return null;},ignored->{message.accept("Record updated.");refresh.run();});
                }
            });
        }));
        panel.add(button.apply("Delete selected record",()->{
            var selected=selected();if(selected==null)return;var type=(RecordService.Kind)kind.getSelectedItem();
            String warning="Delete "+selected+"? "+(type==RecordService.Kind.PROJECT?"Its updates, tasks and follow-ups will also be deleted. ":"")+"A recovery backup will be saved first.";
            if(JOptionPane.showConfirmDialog(this,warning,"Confirm deletion",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.OK_OPTION)
                runner.run(()->records.delete(type,selected,services.backups()),result->{message.accept("Deleted. Recovery backup: "+result);refresh.run();});
        }));return panel;
    }
    private JPanel exportPanel() {
        JPanel panel=vertical();panel.add(new JLabel("Export includes all projects, updates, tasks, follow-ups and import history."));
        for(boolean excel:List.of(true,false))panel.add(button.apply(excel?"Export Excel workbook":"Export CSV files",()->{
            var chooser=new JFileChooser();chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)runner.run(()->new ExportService(services.backups().database()).export(chooser.getSelectedFile().toPath(),excel),result->message.accept("Export saved: "+result));
        }));
        panel.add(new JLabel("Student email for the selected project:"));panel.add(email);
        panel.add(button.apply("Save student email",()->{
            if(projectId==null){message.accept("Select a project first.");return;}String id=projectId,value=email.getText().trim();
            runner.run(()->{var p=settings.load();p.setProperty("email."+id,value);settings.save(p);return p;},result->{preferences=(Properties)result;message.accept("Student email saved on this device.");});
        }));
        panel.add(button.apply("Open reminder draft",()->{
            if(projectId==null){message.accept("Select a project first.");return;}String id=projectId,address=email.getText(),form=link.getText();
            runner.run(()->{var project=services.projects().get(id);return new ReminderService().draft(address,project.studentName(),id,"Please send your weekly progress update and any pending work.",form);},result->open((URI)result));
        }));panel.add(new JLabel("Your default email application opens a draft. You review and send it yourself."));return panel;
    }
    private JPanel settingsPanel() {
        JPanel panel=vertical();panel.add(new JLabel("The update threshold above is saved here and used by weekly reporting too."));
        panel.add(new JLabel("Hosted form server (HTTPS):"));panel.add(server);
        panel.add(button.apply("Save preferences",()->{
            int days=(Integer)threshold.getValue();String url=server.getText().trim();
            runner.run(()->{if(!url.startsWith("https://"))throw new IllegalArgumentException("Use an HTTPS form server.");var p=settings.load();p.setProperty("staleDays",String.valueOf(days));p.setProperty("formServer",url);settings.save(p);return p;},result->{preferences=(Properties)result;message.accept("Preferences saved for future launches.");refresh.run();});
        }));
        panel.add(new JLabel("Weekly report day and local time (HH:mm):"));var schedule=new JPanel(new FlowLayout(FlowLayout.LEFT));schedule.add(day);schedule.add(time);panel.add(schedule);
        panel.add(button.apply("Enable / update weekly reports",()->schedule(false)));panel.add(button.apply("Disable weekly reports",()->schedule(true)));
        panel.add(new JLabel("Online classroom: only project IDs are registered; students submit progress notes."));
        panel.add(button.apply("Create / refresh online classroom",()->runner.run(()->new ClassroomService(settings,services.projects()).connect(),result->{link.setText(result.toString());message.accept("Share this student link: "+result);}))); 
        link.setEditable(false);panel.add(link);
        panel.add(button.apply("Open student submission form",()->{if(link.getText().isBlank())message.accept("Connect the classroom first.");else open(URI.create(link.getText()));}));
        panel.add(button.apply("Review / import student responses",()->reviewResponses()));return panel;
    }
    private void reviewResponses(){
        runner.run(()->{
            Path file=new ClassroomService(settings,services.projects()).download(services.backups().database().path().getParent().resolve("inbox"));
            var rows=new ArrayList<List<String>>();
            try(var reader=java.nio.file.Files.newBufferedReader(file);var csv=org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)){
                for(var row:csv)rows.add(row.toList());
            }
            return Map.entry(file,rows);
        },result->{
            @SuppressWarnings("unchecked") var data=(Map.Entry<Path,List<List<String>>>)result;
            var model=new javax.swing.table.DefaultTableModel(new String[]{"Project","Date","Progress","Next steps"},0){public boolean isCellEditable(int r,int c){return false;}};
            for(var row:data.getValue())model.addRow(new Object[]{row.get(0),row.get(5),row.get(6),row.get(7)});
            var table=new JTable(model);table.setRowHeight(26);table.selectAll();
            var detail=new JTextArea(6,70);detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);
            table.getSelectionModel().addListSelectionListener(event->{int row=table.getSelectedRow();if(row>=0)detail.setText("Progress: "+model.getValueAt(row,2)+"\n\nNext steps: "+model.getValueAt(row,3));});
            var view=new JPanel(new BorderLayout(8,8));view.add(new JLabel("Select responses to import or discard. Full notes appear below; Ctrl-click selects individual rows."),BorderLayout.NORTH);
            var grid=new JScrollPane(table);grid.setPreferredSize(new Dimension(800,250));view.add(grid,BorderLayout.CENTER);view.add(new JScrollPane(detail),BorderLayout.SOUTH);
            int choice=JOptionPane.showOptionDialog(this,view,"Review student responses",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[]{"Import selected","Discard selected","Cancel"},"Import selected");
            int[] selected=table.getSelectedRows();
            if(choice<0||choice==2)return;
            if(selected.length==0){message.accept("No responses selected.");return;}
            if(choice==1&&JOptionPane.showConfirmDialog(this,"Discard selected online responses without importing? A local CSV copy is kept in the inbox folder.","Confirm discard",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            runner.run(()->{
                Path subset=java.nio.file.Files.createTempFile(data.getKey().getParent(),"reviewed-",".csv");
                try(var writer=java.nio.file.Files.newBufferedWriter(subset);var csv=new org.apache.commons.csv.CSVPrinter(writer,org.apache.commons.csv.CSVFormat.DEFAULT)){
                    csv.printRecord(in.iitm.cs5013.tracker.importer.ImportData.COLUMNS);for(int index:selected)csv.printRecord(data.getValue().get(index));
                }
                if(choice==0)services.importer().importFile(subset,false);
                try{new ClassroomService(settings,services.projects()).acknowledge(subset);return choice==0?"Selected responses imported; processed online copies removed.":"Selected responses discarded online. A local copy is kept at "+subset;}
                catch(Exception exception){return choice==0?"Imported locally. Online cleanup failed; retry later. Existing IDs will be skipped.":"Could not remove online copies. Local archive: "+subset;}
            },text->{message.accept(text.toString());refresh.run();});
        });
    }
    private void schedule(boolean remove){String weekday=Objects.toString(day.getSelectedItem()),at=time.getText().trim();
        if(JOptionPane.showConfirmDialog(this,remove?"Disable automatic weekly reports?":"Save a weekly Windows task for "+weekday+" at "+at+"?","Weekly reports",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        runner.run(()->new ScheduleService(services.backups().database().path().getParent()).configure(weekday,at,remove),result->message.accept(result.toString()));
    }
    private void loadRecords(){
        var type=(RecordService.Kind)kind.getSelectedItem();String id=projectId;
        if(type==null||id==null){recordId.removeAllItems();return;}
        runner.run(()->{
            var ids=new ArrayList<String>();
            try(var connection=services.backups().database().open();var statement=connection.prepareStatement("SELECT id FROM "+type.table+" WHERE "+(type==RecordService.Kind.PROJECT?"id":"project_id")+"=? ORDER BY id")) {
                statement.setString(1,id);try(var rows=statement.executeQuery()){while(rows.next())ids.add(rows.getString(1));}
            }return ids;
        },result->{recordId.removeAllItems();for(Object value:(List<?>)result)recordId.addItem(value.toString());});
    }
    private String selected(){String id=(String)recordId.getSelectedItem();if(id==null)message.accept("Select a record first.");return id;}
    private void open(URI uri){runner.run(()->{if(!Desktop.isDesktopSupported())throw new IllegalArgumentException("No desktop application handler is available.");if(uri.getScheme().equals("mailto"))Desktop.getDesktop().mail(uri);else Desktop.getDesktop().browse(uri);return null;},ignored->message.accept("Opened in your default application. Nothing was sent automatically."));}
    private static JPanel vertical(){
        var panel=new JPanel(){protected void addImpl(Component child,Object constraints,int index){
            if(child instanceof JComponent component){component.setAlignmentX(LEFT_ALIGNMENT);component.setMaximumSize(new Dimension(child instanceof JButton?component.getPreferredSize().width:Integer.MAX_VALUE,component.getPreferredSize().height));}
            super.addImpl(child,constraints,index);
            super.addImpl(Box.createVerticalStrut(8),null,-1);
        }};
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));return panel;
    }
    public void setControlsEnabled(boolean enabled){enable(this,enabled);}
    private static void enable(Container parent,boolean enabled){for(var child:parent.getComponents()){
        if(child instanceof JTextField||child instanceof JComboBox<?>||child instanceof JSpinner)child.setEnabled(enabled);
        if(child instanceof Container container)enable(container,enabled);
    }}
}
