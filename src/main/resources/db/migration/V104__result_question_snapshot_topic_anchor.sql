alter table result_question_snapshot
    add column if not exists topic_id_snapshot bigint null;

update result_question_snapshot rqs
set topic_id_snapshot = q.topic_id
from question q
where rqs.question_original_id = q.id
  and rqs.topic_id_snapshot is null;

create index if not exists ix_result_q_snap__topic_id_snapshot
    on result_question_snapshot (topic_id_snapshot);
