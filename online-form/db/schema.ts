// Intentionally empty by default.
// Add Drizzle tables here when the site actually needs a database.
// See examples/d1/db/schema.ts for an opt-in example.
import { sqliteTable, text, integer, index } from "drizzle-orm/sqlite-core";
export const classrooms=sqliteTable("classrooms",{
  id:text("id").primaryKey(),projectIds:text("project_ids").notNull(),createdAt:integer("created_at").notNull()
});
export const submissions=sqliteTable("submissions",{
  id:text("id").primaryKey(),classroomId:text("classroom_id").notNull().references(()=>classrooms.id),
  projectId:text("project_id").notNull(),updateDate:text("update_date").notNull(),summary:text("summary").notNull(),
  nextSteps:text("next_steps").notNull(),receivedAt:integer("received_at").notNull(),sourceHash:text("source_hash").notNull()
},table=>[index("submissions_classroom_time").on(table.classroomId,table.receivedAt)]);
