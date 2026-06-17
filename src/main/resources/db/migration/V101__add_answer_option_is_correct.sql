alter table answer_option
    add column is_correct boolean null;

update answer_option
set is_correct = false
where answer_option_role = 'CHOICE_OPTION'
  and is_correct is null;

alter table answer_option
    add constraint chk_ans_opt__is_correct_applicability
        check (
            (answer_option_role = 'CHOICE_OPTION' and is_correct is not null)
                or
            (answer_option_role in ('MATCH_LEFT', 'MATCH_RIGHT', 'ORDER_ITEM') and is_correct is null)
            );