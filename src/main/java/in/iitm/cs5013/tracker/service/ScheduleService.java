package in.iitm.cs5013.tracker.service;

import java.nio.file.*;
import java.util.*;

public final class ScheduleService {
    private final Path dataDirectory;
    public ScheduleService(Path dataDirectory){this.dataDirectory=dataDirectory;}
    public String configure(String day,String time,boolean remove) throws Exception {
        if(Boolean.getBoolean("tracker.demo")) throw new IllegalArgumentException("Weekly scheduling is disabled in demo mode. Use the installed working application.");
        if(!List.of("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday").contains(day)
                ||!time.matches("([01]\\d|2[0-3]):[0-5]\\d"))throw new IllegalArgumentException("Choose a weekday and time in HH:mm format.");
        Path code=Path.of(ScheduleService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path root=code.getParent()!=null&&code.getParent().getFileName().toString().equals("app")?code.getParent().getParent():null;
        if(root==null||!Files.exists(root.resolve("runtime/bin/java.exe")))throw new IllegalArgumentException("Install the Windows package before configuring weekly reports.");
        Path script=root.resolve("scripts/schedule-weekly-report.ps1");
        var command=new ArrayList<>(List.of("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-ExecutionPolicy","Bypass","-File",script.toString(),"-Day",day,"-Time",time,"-DataDirectory",dataDirectory.toAbsolutePath().toString()));
        command.add(remove?"-Remove":"-Replace");
        var process=new ProcessBuilder(command).redirectErrorStream(true).start();
        String result=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        if(process.waitFor()!=0)throw new IllegalArgumentException("Windows could not update the schedule: "+result);
        return result;
    }
}
