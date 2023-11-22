create schema document_management;
--;;
create table document_management.documents(
	document_id uuid constraint pk_documents primary key,
	title varchar(100),
	type varchar(100),
	origin varchar(100),
	origin_id uuid,
	company_id uuid,
	is_personal boolean,
	is_from_wowtower boolean,
	is_from_company boolean,
	is_archived boolean,
	archive_date timestamp(1) without time zone,
	archive_note varchar(500),
	created_by uuid,
	created_at timestamp(1) without time zone,
	updated_at timestamp(1) without time zone
);
--;;
