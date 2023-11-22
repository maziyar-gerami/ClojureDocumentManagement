create table document_management.word_subsections(
	subsection_id uuid,
	section_id uuid,
	order_number int,
	version varchar(10),
	document_id uuid,
	title varchar(100),
	is_archived boolean,
	is_locked boolean,
	is_visible boolean,
	content varchar,
	created_at timestamp(1) without time zone,
	created_by uuid
);	
--;;
