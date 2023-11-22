create table document_management.word_versions(
	version varchar(10),
	document_id uuid,
	is_published boolean,
	publish_date timestamp(1) without time zone,
	publish_note varchar(500),
	created_at timestamp(1) without time zone,
	created_by uuid
);
--;;
