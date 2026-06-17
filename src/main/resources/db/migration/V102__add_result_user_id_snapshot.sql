alter table result
    add column user_id_snapshot bigint;

update result r
set user_id_snapshot = ta.user_id
from test_attempt ta
where ta.id = r.test_attempt_id
  and r.user_id_snapshot is null;

alter table result
    alter column user_id_snapshot set not null;

create index ix_result__user_id_snap
    on result (user_id_snapshot);
