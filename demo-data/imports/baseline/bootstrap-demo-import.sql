insert into app_role (code, name, description, created_at, updated_at)
values
  ('ADMIN', 'Administrator', 'Dev demo bootstrap role', now(), now()),
  ('EXPERT', 'Expert', 'Dev demo bootstrap role', now(), now()),
  ('OPERATOR', 'Operator', 'Dev demo bootstrap role', now(), now())
on conflict (code) do nothing;

insert into app_user (employee_number, external_id, last_name, first_name, middle_name, status, created_at, updated_at)
values
  ('DEMO-ADMIN-001', 'demo-admin-001', 'Demo', 'Admin', null, 'ACTIVE', now(), now())
on conflict (employee_number) do nothing;

insert into user_role_assignment (user_id, role_id, valid_from, valid_to, created_at, updated_at)
select u.id, r.id, now(), null, now(), now()
from app_user u
join app_role r on r.code = 'ADMIN'
where u.employee_number = 'DEMO-ADMIN-001'
  and not exists (
    select 1
    from user_role_assignment x
    where x.user_id = u.id
      and x.role_id = r.id
      and x.valid_to is null
  );

select u.id, u.employee_number, r.code
from app_user u
join user_role_assignment ura on ura.user_id = u.id and ura.valid_to is null
join app_role r on r.id = ura.role_id
where u.employee_number = 'DEMO-ADMIN-001';
