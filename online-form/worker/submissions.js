const headers={"Cache-Control":"no-store","X-Content-Type-Options":"nosniff"};
const text=(body,status=200)=>new Response(body,{status,headers:{...headers,"Content-Type":"text/plain; charset=utf-8"}});
const hash=async value=>Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(value)))).map(x=>x.toString(16).padStart(2,"0")).join("");
const csv=value=>'"'+String(value??"").replaceAll('"','""')+'"';
export async function submissionsApi(request,env){
  const url=new URL(request.url),now=Math.floor(Date.now()/1000);
  if(!env.DB)return text("Submission storage is unavailable.",503);
  if(url.pathname==="/api/classroom"||url.pathname==="/api/responses"){
    const key=request.headers.get("Authorization")?.replace(/^Bearer /,"")??"";
    if(!/^[A-Za-z0-9_-]{43}$/.test(key))return text("Teacher connection required.",401);
    const classroom=(await hash(key)).slice(0,32);
    if(url.pathname==="/api/classroom"&&request.method==="POST"){
      const body=await request.text();if(body.length>30000)return text("Too many project IDs.",413);
      const ids=[...new Set(body.trim().split(/\r?\n/))];
      if(!ids.length||ids.length>500||ids.some(id=>!id||id.length>100||/[\r\n]/.test(id)))return text("Invalid project IDs.",400);
      await env.DB.prepare("INSERT INTO classrooms(id,project_ids,created_at) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET project_ids=excluded.project_ids").bind(classroom,JSON.stringify(ids),now).run();
      return text("Connected.");
    }
    if(url.pathname==="/api/responses"&&request.method==="GET"){
      const exists=await env.DB.prepare("SELECT id FROM classrooms WHERE id=?").bind(classroom).first();
      if(!exists)return text("Create the classroom first.",404);
      const result=await env.DB.prepare("SELECT id,project_id,update_date,summary,next_steps FROM submissions WHERE classroom_id=? ORDER BY received_at,id LIMIT 5000").bind(classroom).all();
      const rows=[["project_id","update_id","update_date","summary","next_steps"],...result.results.map(row=>[row.project_id,row.id,row.update_date,row.summary,row.next_steps])];
      return new Response(rows.map(row=>row.map(csv).join(",")).join("\r\n")+"\r\n",{headers:{...headers,"Content-Type":"text/csv; charset=utf-8"}});
    }
    if(url.pathname==="/api/responses"&&request.method==="DELETE"){
      const body=await request.text();if(body.length>300000)return text("Too many IDs.",413);
      const ids=[...new Set(body.trim().split(/\r?\n/))];
      if(ids.length>5000||ids.some(id=>!/^S-[a-f0-9-]{36}$/.test(id)))return text("Invalid response IDs.",400);
      for(let i=0;i<ids.length;i+=50)await env.DB.batch(ids.slice(i,i+50).map(id=>env.DB.prepare("DELETE FROM submissions WHERE classroom_id=? AND id=?").bind(classroom,id)));
      return text("Processed copies removed.");
    }
    return text("Method not allowed.",405);
  }
  if(url.pathname==="/api/submit"&&request.method==="POST"){
    if(request.headers.get("Origin")!==url.origin)return text("Submit from the classroom form.",403);
    const body=await request.text();if(body.length>20000)return text("Please shorten your update.",413);
    const form=new URLSearchParams(body);
    const classroom=form.get("classroom")??"",project=(form.get("project_id")??"").trim(),date=form.get("update_date")??"";
    const summary=(form.get("summary")??"").trim(),next=(form.get("next_steps")??"").trim(),id=form.get("submission_id")??"";
    if(!/^[a-f0-9]{32}$/.test(classroom)||!/^S-[a-f0-9-]{36}$/.test(id)||!/^\d{4}-\d{2}-\d{2}$/.test(date)
      ||!summary||summary.length>6000||next.length>3000||!project||project.length>100||form.get("website"))return text("Check your project ID, date and update text.",400);
    const parsed=new Date(date+"T00:00:00Z");
    if(!Number.isFinite(parsed.getTime())||parsed.toISOString().slice(0,10)!==date||date>new Date(Date.now()+19800000).toISOString().slice(0,10))return text("Choose a valid update date, not a future date.",400);
    const group=await env.DB.prepare("SELECT project_ids FROM classrooms WHERE id=?").bind(classroom).first();
    if(!group||!JSON.parse(group.project_ids).includes(project))return text("Project ID not registered. Check it with your supervisor.",400);
    const existing=await env.DB.prepare("SELECT id FROM submissions WHERE id=? AND classroom_id=?").bind(id,classroom).first();
    if(existing)return Response.redirect(url.origin+"/s/"+classroom+"?saved=1",303);
    const source=await hash(classroom+":"+(request.headers.get("CF-Connecting-IP")??"unknown"));
    const recent=await env.DB.prepare("SELECT COUNT(*) AS n FROM submissions WHERE classroom_id=? AND received_at>? AND (source_hash=? OR project_id=?)").bind(classroom,now-3600,source,project).first();
    if(Number(recent.n)>=20)return text("Too many updates recently. Please try again later.",429);
    await env.DB.prepare("INSERT INTO submissions(id,classroom_id,project_id,update_date,summary,next_steps,received_at,source_hash) VALUES(?,?,?,?,?,?,?,?)").bind(id,classroom,project,date,summary,next,now,source).run();
    return Response.redirect(url.origin+"/s/"+classroom+"?saved=1",303);
  }
  return text("Not found.",404);
}
