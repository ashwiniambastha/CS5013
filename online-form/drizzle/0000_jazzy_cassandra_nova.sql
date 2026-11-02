CREATE TABLE `classrooms` (
	`id` text PRIMARY KEY NOT NULL,
	`project_ids` text NOT NULL,
	`created_at` integer NOT NULL
);
--> statement-breakpoint
CREATE TABLE `submissions` (
	`id` text PRIMARY KEY NOT NULL,
	`classroom_id` text NOT NULL,
	`project_id` text NOT NULL,
	`update_date` text NOT NULL,
	`summary` text NOT NULL,
	`next_steps` text NOT NULL,
	`received_at` integer NOT NULL,
	`source_hash` text NOT NULL,
	FOREIGN KEY (`classroom_id`) REFERENCES `classrooms`(`id`) ON UPDATE no action ON DELETE no action
);
--> statement-breakpoint
CREATE INDEX `submissions_classroom_time` ON `submissions` (`classroom_id`,`received_at`);