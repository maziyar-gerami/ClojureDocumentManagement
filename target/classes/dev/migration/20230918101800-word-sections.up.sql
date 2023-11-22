create table document_management.word_sections(
	section_id uuid,
	order_number int,
	version varchar(10),
	document_id uuid,
	title varchar(100),
	is_private boolean,
	is_archived boolean,
	is_locked boolean,
	is_visible boolean,
	created_at timestamp(1) without time zone,
	created_by uuid
);
--;;
