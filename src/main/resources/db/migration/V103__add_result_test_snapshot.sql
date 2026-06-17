alter table result
    add column test_id_snapshot bigint,
    add column test_name_snapshot text;

update result r
set test_id_snapshot = ta.test_id,
    test_name_snapshot = t.name
from test_attempt ta
join test t on t.id = ta.test_id
where ta.id = r.test_attempt_id
  and r.test_id_snapshot is null;

alter table result
    alter column test_id_snapshot set not null,
    alter column test_name_snapshot set not null;

alter table result
    add constraint chk_result__test_name_snapshot_not_blank
        check (btrim(test_name_snapshot) <> '');

create index ix_result__test_id_snap
    on result (test_id_snapshot);
