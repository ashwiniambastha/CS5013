import in.iitm.cs5013.tracker.repository.*;
import in.iitm.cs5013.tracker.service.*;
import java.nio.file.*;
import java.time.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Uses only synthetic test records; credentials never leave encrypted preferences. */
public class OnlineClassroomSmoke {
 public static void main(String[] args)throws Exception{
  Path directory=Path.of(args[0]);var database=new Database(directory.resolve("tracker.db"));database.initialize();
  var clock=Clock.systemDefaultZone();var projects=new ProjectService(new SqliteProjectRepository(database),clock);
  projects.add("SMOKE-001","Synthetic student","Hosted submission check",LocalDate.now().minusDays(10));
  var settings=new SettingsService(directory);var classroom=new ClassroomService(settings,projects);
  String url=classroom.connect(),id=url.substring(url.lastIndexOf('/')+1),updateId="S-"+UUID.randomUUID();
  String origin=URI.create(url).getScheme()+"://"+URI.create(url).getHost();
  var values=new LinkedHashMap<String,String>();values.put("classroom",id);values.put("project_id","SMOKE-001");values.put("submission_id",updateId);
  values.put("update_date",LocalDate.now().minusDays(1).toString());values.put("summary","Synthetic end-to-end test; no personal information");values.put("next_steps","Complete verification");
  String body=String.join("&",values.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).toList());
  var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(origin+"/api/submit")).header("Origin",origin).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()!=303)throw new IllegalStateException("Submission failed: "+response.statusCode()+" "+response.body());
  Path csv=classroom.download(directory.resolve("inbox"));var importer=new ImportService(new SqliteImportRepository(database),clock);
  if(importer.importFile(csv,true).updatesAdded()!=1)throw new IllegalStateException("Preview did not find the response.");
  if(!projects.updates("SMOKE-001").isEmpty())throw new IllegalStateException("Preview wrote records.");
  importer.importFile(csv,false);classroom.acknowledge(csv);
  if(projects.updates("SMOKE-001").size()!=1)throw new IllegalStateException("Response not imported.");
  try{classroom.download(directory.resolve("inbox"));throw new IllegalStateException("Processed response still present online.");}
  catch(IllegalArgumentException expected){if(!expected.getMessage().contains("No student responses"))throw expected;}
  System.out.println("PASS: encrypted classroom connection, public submission, private download, preview, import and online acknowledgement.");
  System.out.println("Synthetic student form: "+url);
 }
}
