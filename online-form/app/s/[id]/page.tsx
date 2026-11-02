export const dynamic="force-dynamic";
export default async function SubmissionPage({params,searchParams}: {params:Promise<{id:string}>,searchParams:Promise<{saved?:string}>}) {
 const {id}=await params;const {saved}=await searchParams;
 if(!/^[a-f0-9]{32}$/.test(id))return <main><h1>Invalid classroom link</h1><p>Ask your supervisor for the correct link.</p></main>;
 return <main style={{maxWidth:640,margin:"48px auto",padding:"0 24px",fontFamily:"system-ui",lineHeight:1.6}}>
  <p style={{color:"#22645a",fontWeight:700}}>WEEKLY PROJECT UPDATE</p><h1 style={{fontSize:36,lineHeight:1.15}}>Keep your supervisor<br/>up to date.</h1>
  <p>Your update goes to your supervisor for review. You cannot view anyone else’s records.</p>
  {saved&&<p role="status" style={{padding:16,background:"#e4f5e9"}}>Your update was received. Your supervisor will review it.</p>}
  <form method="post" action="/api/submit" style={{display:"grid",gap:18}}>
   <input type="hidden" name="classroom" value={id}/><input type="hidden" name="submission_id" value={"S-"+crypto.randomUUID()}/>
   <input name="website" tabIndex={-1} autoComplete="off" aria-hidden="true" style={{display:"none"}}/>
   <label>Project ID<input name="project_id" required maxLength={100} placeholder="Given by your supervisor" style={{display:"block",padding:12,border:"1px solid #b4c9c3",borderRadius:6,background:"#f8fbfa",width:"100%",boxSizing:"border-box"}}/></label>
   <label>Update date<input type="date" name="update_date" required max={new Date(Date.now()+19800000).toISOString().slice(0,10)} style={{display:"block",padding:12,border:"1px solid #b4c9c3",borderRadius:6,background:"#f8fbfa"}}/></label>
   <label>What progress have you made?<textarea name="summary" required maxLength={6000} rows={3} style={{display:"block",width:"100%",padding:12,border:"1px solid #b4c9c3",borderRadius:6,background:"#f8fbfa",boxSizing:"border-box"}}/></label>
   <label>What are your next steps?<textarea name="next_steps" maxLength={3000} rows={3} style={{display:"block",width:"100%",padding:12,border:"1px solid #b4c9c3",borderRadius:6,background:"#f8fbfa",boxSizing:"border-box"}}/></label>
   <button style={{padding:16,background:"#17584e",color:"white",border:0,fontSize:17,borderRadius:6}}>Submit update</button>
  </form><p style={{fontSize:13,color:"#555"}}>Do not include passwords or sensitive personal information. Your progress notes are stored online until your supervisor reviews them. Contact your supervisor to correct or remove a submission.</p>
 </main>;
}
