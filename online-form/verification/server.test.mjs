import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {submissionsApi} from '../worker/submissions.js';
import {createHash,randomUUID} from 'node:crypto';

test('classroom keys isolate records; submission validation, deduplication and acknowledgement',async()=>{
 const sql=new DatabaseSync(':memory:');
 sql.exec('CREATE TABLE classrooms(id TEXT PRIMARY KEY,project_ids TEXT,created_at INTEGER); CREATE TABLE submissions(id TEXT PRIMARY KEY,classroom_id TEXT,project_id TEXT,update_date TEXT,summary TEXT,next_steps TEXT,received_at INTEGER,source_hash TEXT)');
 const DB={prepare(query){let values=[];return{bind(...args){values=args;return this},async first(){return sql.prepare(query).get(...values)},async all(){return{results:sql.prepare(query).all(...values)}},async run(){return sql.prepare(query).run(...values)}}},async batch(statements){return Promise.all(statements.map(s=>s.run()))}};
 const key='a'.repeat(43),other='b'.repeat(43),group=createHash('sha256').update(key).digest('hex').slice(0,32),id='S-'+randomUUID();
 const request=(path,options={})=>new Request('https://example.test'+path,options);
 assert.equal((await submissionsApi(request('/api/responses'),{DB})).status,401);
 assert.equal((await submissionsApi(request('/api/classroom',{method:'POST',headers:{Authorization:'Bearer '+key},body:'P001'}),{DB})).status,200);
 const data=new URLSearchParams({classroom:group,project_id:'P001',submission_id:id,update_date:'2026-10-01',summary:'Read paper',next_steps:'Draft proof'});
 const post=()=>request('/api/submit',{method:'POST',headers:{Origin:'https://example.test'},body:data});
 assert.equal((await submissionsApi(post(),{DB})).status,303);
 assert.equal((await submissionsApi(post(),{DB})).status,303);
 assert.equal(sql.prepare('SELECT COUNT(*) AS n FROM submissions').get().n,1);
 const response=await submissionsApi(request('/api/responses',{headers:{Authorization:'Bearer '+key}}),{DB});
 assert.match(await response.text(),/Read paper/);
 assert.equal((await submissionsApi(request('/api/responses',{headers:{Authorization:'Bearer '+other}}),{DB})).status,404);
 data.set('project_id','UNKNOWN');data.set('submission_id','S-'+randomUUID());
 assert.equal((await submissionsApi(post(),{DB})).status,400);
 await submissionsApi(request('/api/responses',{method:'DELETE',headers:{Authorization:'Bearer '+other},body:id}),{DB});
 assert.equal(sql.prepare('SELECT COUNT(*) AS n FROM submissions').get().n,1);
 await submissionsApi(request('/api/responses',{method:'DELETE',headers:{Authorization:'Bearer '+key},body:id}),{DB});
 assert.equal(sql.prepare('SELECT COUNT(*) AS n FROM submissions').get().n,0);sql.close();
});
