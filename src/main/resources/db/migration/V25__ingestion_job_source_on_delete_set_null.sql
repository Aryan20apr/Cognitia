ALTER TABLE ingestion_job DROP CONSTRAINT IF EXISTS fkcq1at9d04rdj1e7k7kmc22glf;

ALTER TABLE ingestion_job
    ADD CONSTRAINT fk_ingestion_job_source
    FOREIGN KEY (res_id) REFERENCES raw_resource(res_id)
    ON DELETE SET NULL;
