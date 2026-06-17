import { useQueries } from '@tanstack/react-query';
import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import { Link } from 'react-router';
import { useAssignedLearningList } from '../features/assigned-learning/model/useAssignedLearning';
import {
  canAccessAssignmentCampaignArea,
  canAccessAdministrationArea,
  canAccessExpertArea,
  canAccessManagerArea,
  hasExtendedSection,
} from '../features/auth/model/currentActor';
import { useCurrentActor } from '../features/auth/model/useCurrentActor';
import { getQuestionsByTopic, getTopicsByCourse } from '../features/expert-content/api/expertContentApi';
import { useCourses } from '../features/expert-content/model/useExpertContent';
import { useExpertQuestionAnalytics } from '../features/expert-analytics/model/useExpertAnalytics';
import {
  useManagerialCurrentSupervision,
  useManagerialDepartmentTopicAnalytics,
} from '../features/managerial/model/useManagerial';
import { useSelfNotifications } from '../features/notifications/model/useNotifications';
import { useSelfResultHistory } from '../features/self-results/model/useSelfResults';
import { useSelfTestingCatalog } from '../features/self-testing/model/useSelfTesting';
import { formatPercent, formatUiDate, localizeRole } from '../shared/ui/presentation';
import { humanizeOrganizationalUnit } from '../shared/ui/organizationalUnits';

type ManagerQueueItem = {
  employeeName: string;
  courseName: string;
  deadlineAt?: string;
  status: string;
};

export function HomePage() {
  const { data: actor } = useCurrentActor();
  const enabledSections = actor?.enabledSections ?? [];
  const roles = actor?.roles ?? [];
  const hasAssignedLearning = enabledSections.includes('ASSIGNED_LEARNING');
  const hasSelfResults = enabledSections.includes('SELF_RESULTS');
  const hasSelfTesting = enabledSections.includes('SELF_TESTING');
  const hasAssignmentAdminArea = actor ? canAccessAssignmentCampaignArea(actor) : false;
  const hasAdministrationArea = actor ? canAccessAdministrationArea(actor) : false;
  const hasManagerArea = actor ? canAccessManagerArea(actor) : false;
  const hasExpertArea = actor ? canAccessExpertArea(actor) : false;
  const hasExpertContent = actor ? hasExtendedSection(actor, 'EXPERT_CONTENT') : false;
  const hasExpertAnalytics = actor ? hasExtendedSection(actor, 'EXPERT_QUESTION_ANALYTICS') : false;
  const isManagerHome = hasManagerArea && !hasExpertArea;

  const currentYearStart = dayjs().startOf('year').toISOString();
  const currentYearEnd = dayjs().endOf('year').toISOString();

  const assignedQuery = useAssignedLearningList(hasAssignedLearning && !isManagerHome);
  const selfTestingQuery = useSelfTestingCatalog(hasSelfTesting && !isManagerHome);
  const selfResultsQuery = useSelfResultHistory(hasSelfResults && !isManagerHome);
  const selfNotificationsQuery = useSelfNotifications(!hasExpertArea);
  const currentSupervisionQuery = useManagerialCurrentSupervision(isManagerHome);
  const departmentAnalyticsQuery = useManagerialDepartmentTopicAnalytics(
    currentYearStart,
    currentYearEnd,
    isManagerHome,
  );
  const expertCoursesQuery = useCourses(hasExpertArea && hasExpertContent);
  const expertQuestionAnalyticsQuery = useExpertQuestionAnalytics(
    currentYearStart,
    currentYearEnd,
    hasExpertArea && hasExpertAnalytics,
  );

  const displayName = actor?.displayName ?? 'Сотрудник';
  const positionTitle = actor?.positionTitle?.trim() || undefined;
  const employeeNumber = actor?.employeeNumber ?? 'Не указано';
  const installation = humanizeOrganizationalUnit(
    actor?.primaryOrganizationalUnitName,
    actor?.primaryOrganizationalUnitPath,
  );
  const assignedCount = hasAssignedLearning ? (assignedQuery.data?.length ?? 0) : 0;
  const selfTestingCount = hasSelfTesting ? (selfTestingQuery.data?.length ?? 0) : 0;
  const latestResult = selfResultsQuery.data?.[0];
  const recentNotifications = (selfNotificationsQuery.data ?? []).slice(0, 4);

  const expertTopicQueries = useQueries({
    queries: (expertCoursesQuery.data ?? []).map((course) => ({
      queryKey: ['home-expert-topics', course.id],
      queryFn: () => getTopicsByCourse(course.id),
      enabled: hasExpertArea && hasExpertContent,
    })),
  });
  const expertTopics = expertTopicQueries.flatMap((query) => query.data ?? []);
  const expertQuestionQueries = useQueries({
    queries: expertTopics.map((topic) => ({
      queryKey: ['home-expert-topic-questions', topic.id],
      queryFn: () => getQuestionsByTopic(topic.id),
      enabled: hasExpertArea && hasExpertContent,
    })),
  });
  const expertQuestions = expertQuestionQueries.flatMap((query) => query.data ?? []);
  const questionToTopic = new Map(expertQuestions.map((question) => [question.id, question.topicId]));
  const topicById = new Map(expertTopics.map((topic) => [topic.id, topic]));
  const courseById = new Map((expertCoursesQuery.data ?? []).map((course) => [course.id, course]));
  const expertWeakTopics = Object.values(
    (expertQuestionAnalyticsQuery.data ?? []).reduce<Record<string, {
      topicId: number;
      topicName: string;
      courseName: string;
      attemptCount: number;
      correctCount: number;
      incorrectCount: number;
    }>>((accumulator, item) => {
      const topicId = questionToTopic.get(item.questionId);
      if (!topicId) {
        return accumulator;
      }
      const topic = topicById.get(topicId);
      if (!topic) {
        return accumulator;
      }
      const course = courseById.get(topic.courseId);
      const key = String(topicId);
      if (!accumulator[key]) {
        accumulator[key] = {
          topicId,
          topicName: topic.name,
          courseName: course?.name ?? 'Курс не указан',
          attemptCount: 0,
          correctCount: 0,
          incorrectCount: 0,
        };
      }
      accumulator[key].attemptCount += item.attemptCount ?? 0;
      accumulator[key].correctCount += item.correctCount ?? 0;
      accumulator[key].incorrectCount += item.incorrectCount ?? 0;
      return accumulator;
    }, {}),
  )
    .map((item) => {
      const totalAnswers = item.correctCount + item.incorrectCount;
      return {
        ...item,
        passRate: totalAnswers > 0 ? (item.correctCount / totalAnswers) * 100 : 0,
      };
    })
    .filter((item) => item.attemptCount > 0)
    .sort((left, right) => {
      if (left.passRate !== right.passRate) {
        return left.passRate - right.passRate;
      }
      return right.attemptCount - left.attemptCount;
    })
    .slice(0, 5);
  const expertTopicDashboardLoading =
    (hasExpertArea && hasExpertContent && expertCoursesQuery.isLoading) ||
    expertTopicQueries.some((query) => query.isLoading) ||
    expertQuestionQueries.some((query) => query.isLoading) ||
    (hasExpertArea && hasExpertAnalytics && expertQuestionAnalyticsQuery.isLoading);

  if (isManagerHome) {
    const supervisionItems = currentSupervisionQuery.data ?? [];
    const analyticsItems = departmentAnalyticsQuery.data ?? [];
    const managedEmployeesCount = new Set(
      supervisionItems.map((item) => item.userId ?? item.userDisplayName).filter(Boolean),
    ).size;
    const overdueCount = supervisionItems.filter((item) => item.assignmentStatus === 'OVERDUE').length;
    const activeCount = supervisionItems.filter((item) =>
      ['ASSIGNED', 'ACTIVE', 'IN_PROGRESS'].includes(item.assignmentStatus ?? ''),
    ).length;
    const departmentSnapshots = analyticsItems
      .map((item) => ({
        departmentName: humanizeOrganizationalUnit(
          item.organizationalUnitName,
          item.organizationalPathSnapshot,
          'Подразделение без названия',
        ),
        passRate: toPercentNumber(item.passRatePercent),
        averageScore: toPercentNumber(item.averageScorePercent),
        errorCount: item.errorCount ?? 0,
      }))
      .sort((left, right) => {
        if (left.passRate !== right.passRate) {
          return left.passRate - right.passRate;
        }
        return right.errorCount - left.errorCount;
      })
      .slice(0, 3);
    const queueItems = supervisionItems
      .map((item) => ({
        employeeName: item.userDisplayName ?? 'Сотрудник',
        courseName: item.courseName ?? 'Курс без названия',
        deadlineAt: item.deadlineAt,
        status: localizeManagerAssignmentStatus(item.assignmentStatus),
      }))
      .sort(compareQueueItems)
      .slice(0, 4);
    const averagePassRate =
      analyticsItems.length > 0
        ? analyticsItems.reduce((sum, item) => sum + toPercentNumber(item.passRatePercent), 0) / analyticsItems.length
        : 0;

    return (
      <div className="home-page home-page-compact">
        <section className="self-hero home-manager-hero">
          <div className="self-hero-copy">
            <span className="home-card-kicker">Панель руководителя</span>
            <Typography.Title level={2} className="self-hero-title">
              Контроль обучения
            </Typography.Title>
            <Typography.Paragraph className="self-hero-text">
              На главном экране собраны активные назначения, состояние подразделений и быстрые переходы к управленческим разделам.
            </Typography.Paragraph>
            <Space wrap>
              <Link to="/manager/current-supervision">
                <Button type="primary" size="large">
                  Открыть контроль обучения
                </Button>
              </Link>
              <Link to="/manager/analytics/department-topic">
                <Button size="large">Аналитика по подразделениям</Button>
              </Link>
              <Link to="/manager/analytics/user-topic">
                <Button size="large">Аналитика по сотрудникам</Button>
              </Link>
            </Space>
          </div>

          <Card className="self-hero-panel home-manager-panel">
            <Space direction="vertical" size={14} style={{ width: '100%' }}>
              <div className="home-profile-topline">{positionTitle ?? 'Руководитель'}</div>
              <Typography.Title level={3} className="home-profile-name">
                {displayName}
              </Typography.Title>
              <Typography.Paragraph className="home-profile-login">
                Табельный номер: {employeeNumber}
              </Typography.Paragraph>
              <Typography.Paragraph className="home-profile-login">Подразделение: {installation}</Typography.Paragraph>
              <div className="home-role-list">
                {roles.length > 0 ? (
                  roles.map((role) => (
                    <Tag key={role} bordered={false} className="home-role-tag">
                      {localizeRole(role)}
                    </Tag>
                  ))
                ) : (
                  <Tag bordered={false} className="home-role-tag">
                    Руководитель
                  </Tag>
                )}
              </div>
            </Space>
          </Card>
        </section>

        <div className="home-manager-stats">
          <SummaryMetricCard label="Сотрудников в подчинении" value={String(managedEmployeesCount)} />
          <SummaryMetricCard label="Активных назначений" value={String(activeCount)} />
          <SummaryMetricCard label="Просрочено" value={String(overdueCount)} />
          <SummaryMetricCard label="Средний процент прохождения" value={`${Math.round(averagePassRate)}%`} />
        </div>

        <Row gutter={[18, 18]} className="home-main-grid">
          <Col xs={24} xl={14}>
            <div className="home-side-stack">
              <Card className="home-action-card">
                <Typography.Title level={3} className="home-action-title">
                  Куда смотреть в первую очередь
                </Typography.Title>
                <div className="home-guide-list">
                  {departmentSnapshots.length > 0 ? (
                    departmentSnapshots.map((department, index) => (
                      <div key={`${department.departmentName}-${index}`} className="home-guide-item">
                        <span className="home-guide-index">{`0${index + 1}`}</span>
                        <div>
                          <Typography.Text strong>{department.departmentName}</Typography.Text>
                          <Typography.Paragraph className="home-note-text">
                            {`Прохождение ${Math.round(department.passRate)}%, средний результат ${Math.round(department.averageScore)}%, ошибок ${department.errorCount}.`}
                          </Typography.Paragraph>
                        </div>
                      </div>
                    ))
                  ) : (
                    <Typography.Paragraph className="home-note-text">
                      Данные по подразделениям пока не подготовлены.
                    </Typography.Paragraph>
                  )}
                </div>
              </Card>

              <Card className="home-guide-card">
                <Typography.Title level={3} className="home-section-title">
                  Ближайшие задачи
                </Typography.Title>
                <div className="home-guide-list">
                  {queueItems.length > 0 ? (
                    queueItems.map((item, index) => (
                      <div key={`${item.employeeName}-${item.courseName}-${index}`} className="home-guide-item">
                        <span className="home-guide-index">{`0${index + 1}`}</span>
                        <div>
                          <Typography.Text strong>{item.employeeName}</Typography.Text>
                          <Typography.Paragraph className="home-note-text">{item.courseName}</Typography.Paragraph>
                          <Typography.Paragraph className="home-note-text">
                            {`Статус: ${item.status}${item.deadlineAt ? `, срок: ${formatUiDate(item.deadlineAt)}` : ''}`}
                          </Typography.Paragraph>
                        </div>
                      </div>
                    ))
                  ) : (
                    <Typography.Paragraph className="home-note-text">
                      В контуре руководителя пока нет активных назначений.
                    </Typography.Paragraph>
                  )}
                </div>
              </Card>
            </div>
          </Col>

          <Col xs={24} xl={10}>
            <Card className="home-notification-card">
              <div className="home-notification-header">
                <Typography.Title level={3} className="home-section-title">
                  Уведомления
                </Typography.Title>
              </div>
              <div className="home-notification-list">
                {recentNotifications.length > 0 ? (
                  recentNotifications.map((notification) => (
                    <div key={notification.id} className="home-notification-item">
                      <Link to={`/learner/notifications/${notification.id}`} className="home-notification-link">
                        {notification.title || 'Уведомление'}
                      </Link>
                      {notification.message ? (
                        <Typography.Paragraph className="home-note-text">{notification.message}</Typography.Paragraph>
                      ) : null}
                      <Typography.Text type="secondary" className="home-notification-item-date">
                        {formatUiDate(notification.createdAt ?? undefined)}
                      </Typography.Text>
                    </div>
                  ))
                ) : (
                  <Typography.Paragraph className="home-note-text">Новых уведомлений пока нет.</Typography.Paragraph>
                )}
              </div>
            </Card>
          </Col>
        </Row>
      </div>
    );
  }

  if (hasExpertArea) {
    if (hasAdministrationArea) {
      return (
        <div className="home-page home-page-compact">
          <section className="home-admin-profile-row">
            <Card className="home-profile-panel">
              {positionTitle ? <div className="home-profile-topline">{positionTitle}</div> : null}
              <Typography.Title level={3} className="home-profile-name">
                {displayName}
              </Typography.Title>
              <Typography.Paragraph className="home-profile-login">
                Табельный номер: {employeeNumber}
              </Typography.Paragraph>
              <Typography.Paragraph className="home-profile-login">Подразделение: {installation}</Typography.Paragraph>
              <div className="home-role-list">
                {roles.length > 0 ? (
                  roles.map((role) => (
                    <Tag key={role} bordered={false} className="home-role-tag">
                      {localizeRole(role)}
                    </Tag>
                  ))
                ) : (
                  <Tag bordered={false} className="home-role-tag">
                    Администратор
                  </Tag>
                )}
              </div>
            </Card>
          </section>

          <section className="home-admin-hub">
            <Card className="home-admin-group-card">
              <span className="home-summary-label">Люди и структура</span>
              <Typography.Title level={3} className="home-section-title">
                Управление организацией
              </Typography.Title>
              <div className="home-admin-link-grid">
                <AdminHubLink
                  kicker="Профили"
                  title="Пользователи"
                  description="Карточки сотрудников, роли и оргпривязки."
                  to="/admin/users"
                />
                <AdminHubLink
                  kicker="Дерево"
                  title="Оргструктура"
                  description="Подразделения, типы узлов и связи между ними."
                  to="/admin/organization"
                />
                <AdminHubLink
                  kicker="Права"
                  title="Доступы"
                  description="Зоны доступа, делегирование и управленческие связи."
                  to="/admin/access"
                />
              </div>
            </Card>

            <Card className="home-admin-group-card">
              <span className="home-summary-label">Операции</span>
              <Typography.Title level={3} className="home-section-title">
                Системные процессы
              </Typography.Title>
              <div className="home-admin-link-grid">
                <AdminHubLink
                  kicker="Загрузка"
                  title="Импорт"
                  description="Пакетная загрузка данных, dry-run и review-контур."
                  to="/admin/import/jobs"
                />
                <AdminHubLink
                  kicker="Контроль"
                  title="Аудит"
                  description="Журнал административных событий и переход в detail."
                  to="/admin/audit"
                />
                <AdminHubLink
                  kicker="Связь"
                  title="Уведомления"
                  description="Административная лента и контроль системных сообщений."
                  to="/admin/notifications"
                />
              </div>
            </Card>

            <Card className="home-admin-group-card">
              <span className="home-summary-label">Обучение</span>
              <Typography.Title level={3} className="home-section-title">
                Назначения и учебное содержание
              </Typography.Title>
              <div className="home-admin-link-grid">
                <AdminHubLink
                  kicker="Массово"
                  title="Кампании назначений"
                  description="Запуск массовых кампаний и управление охватом."
                  to="/admin/assignment-campaigns"
                />
                <AdminHubLink
                  kicker="Точечно"
                  title="Индивидуальные назначения"
                  description="Ручные назначения отдельным сотрудникам."
                  to="/admin/assignment-individual"
                />
                <AdminHubLink
                  kicker="Поддержка"
                  title="Служебные действия"
                  description="Операции сопровождения назначений и служебные сценарии."
                  to="/admin/assignment-service"
                />
              </div>
            </Card>
          </section>

        </div>
      );
    }

    return (
      <div className="home-page home-page-compact">
        <section className="home-overview-grid">
          <Card className="home-action-card">
            <Typography.Paragraph className="home-action-text">
              На главной собраны только полезные действия и один живой дашборд по темам, где сотрудники отвечают хуже всего.
            </Typography.Paragraph>
            <Space wrap>
              {hasExpertContent ? (
                <Link to="/expert/content/courses">
                  <Button type="primary" size="large">
                    Открыть материалы
                  </Button>
                </Link>
              ) : null}
              {hasAssignmentAdminArea ? (
                <Link to="/admin/assignment-campaigns">
                  <Button size="large">Открыть кампании</Button>
                </Link>
              ) : null}
              {hasExpertAnalytics ? (
                <Link to="/expert/analytics/questions">
                  <Button size="large">Открыть аналитику</Button>
                </Link>
              ) : null}
            </Space>
          </Card>

          <Card className="home-profile-panel">
            {positionTitle ? <div className="home-profile-topline">{positionTitle}</div> : null}
            <Typography.Title level={3} className="home-profile-name">
              {displayName}
            </Typography.Title>
            <Typography.Paragraph className="home-profile-login">
              Табельный номер: {employeeNumber}
            </Typography.Paragraph>
            <Typography.Paragraph className="home-profile-login">Подразделение: {installation}</Typography.Paragraph>
            <div className="home-role-list">
              {roles.length > 0 ? (
                roles.map((role) => (
                  <Tag key={role} bordered={false} className="home-role-tag">
                    {localizeRole(role)}
                  </Tag>
                ))
              ) : (
                <Tag bordered={false} className="home-role-tag">
                  Пользователь
                </Tag>
              )}
            </div>
          </Card>
        </section>

        <Card className="home-guide-card">
          <Typography.Title level={3} className="home-section-title">
            Темы с самым низким процентом правильных ответов
          </Typography.Title>
          {expertTopicDashboardLoading ? (
            <Typography.Paragraph className="home-note-text">
              Загружаем сводку по темам и связываем аналитику вопросов с учебным содержанием.
            </Typography.Paragraph>
          ) : !hasExpertAnalytics ? (
            <Typography.Paragraph className="home-note-text">
              Для этого блока нужен доступ к аналитике по вопросам.
            </Typography.Paragraph>
          ) : !hasExpertContent ? (
            <Typography.Paragraph className="home-note-text">
              Для сборки дашборда нужен доступ к учебным материалам и темам.
            </Typography.Paragraph>
          ) : expertWeakTopics.length > 0 ? (
            <div className="home-guide-list">
              {expertWeakTopics.map((topic, index) => (
                <div key={topic.topicId} className="home-guide-item">
                  <span className="home-guide-index">{`0${index + 1}`}</span>
                  <div>
                    <Typography.Text strong>
                      <Link to={`/expert/content/topics/${topic.topicId}/questions`}>{topic.topicName}</Link>
                    </Typography.Text>
                    <Typography.Paragraph className="home-note-text">{topic.courseName}</Typography.Paragraph>
                    <Typography.Paragraph className="home-note-text">
                      {`Правильных ответов ${formatPercent(topic.passRate)}, попыток ${topic.attemptCount}, ошибок ${topic.incorrectCount}.`}
                    </Typography.Paragraph>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Typography.Paragraph className="home-note-text">
              Пока недостаточно данных, чтобы собрать рейтинг слабых тем за текущий период.
            </Typography.Paragraph>
          )}
        </Card>
      </div>
    );
  }

  return (
    <div className="home-page home-page-compact">
      <section className="home-overview-grid">
        <div className="home-summary-grid">
          <div className="home-summary-card">
            <span className="home-summary-label">Назначенное обучение</span>
            <strong>{assignedCount > 0 ? `${assignedCount} активных модулей` : 'Нет активных назначений'}</strong>
            <Typography.Text className="home-summary-text">
              {hasAssignedLearning
                ? 'Откройте обязательные курсы и проверьте ближайшие сроки.'
                : 'Раздел появится, когда для профиля будут доступны назначенные курсы.'}
            </Typography.Text>
          </div>

          <div className="home-summary-card">
            <span className="home-summary-label">Самостоятельное обучение</span>
            <strong>{selfTestingCount > 0 ? `${selfTestingCount} доступных модулей` : 'Каталог пока пуст'}</strong>
            <Typography.Text className="home-summary-text">
              {hasSelfTesting
                ? 'Выберите модуль для тренировки и проходите его в удобном темпе.'
                : 'Раздел будет доступен после включения самостоятельного обучения для текущего профиля.'}
            </Typography.Text>
          </div>

          <div className="home-summary-card">
            <span className="home-summary-label">Последний результат</span>
            <strong>{latestResult?.testName ?? 'Результатов пока нет'}</strong>
            <Typography.Text className="home-summary-text">
              {latestResult
                ? 'Последняя завершённая попытка уже отражена в истории результатов.'
                : 'После завершения тестов история и аналитика появятся в этом блоке.'}
            </Typography.Text>
          </div>
        </div>

        <Card className="home-profile-panel">
          {positionTitle ? <div className="home-profile-topline">{positionTitle}</div> : null}
          <Typography.Title level={3} className="home-profile-name">
            {displayName}
          </Typography.Title>
          <Typography.Paragraph className="home-profile-login">
            Табельный номер: {employeeNumber}
          </Typography.Paragraph>
          <Typography.Paragraph className="home-profile-login">Подразделение: {installation}</Typography.Paragraph>

          <div className="home-role-list">
            {roles.length > 0 ? (
              roles.map((role) => (
                <Tag key={role} bordered={false} className="home-role-tag">
                  {localizeRole(role)}
                </Tag>
              ))
            ) : (
              <Tag bordered={false} className="home-role-tag">
                Пользователь
              </Tag>
            )}
          </div>
        </Card>
      </section>

      <Row gutter={[18, 18]} className="home-main-grid">
        <Col xs={24} lg={12}>
          <div className="home-side-stack">
            <Card className="home-guide-card">
              <Typography.Title level={3} className="home-section-title">
                Быстрый обзор дня
              </Typography.Title>
              <div className="home-guide-list">
                {hasExpertArea ? (
                  <>
                    <GuideStep
                      index="01"
                      title="Откройте материалы и темы"
                      text="Держите под рукой курсы и быстро переходите к вопросам каждой темы."
                    />
                    <GuideStep
                      index="02"
                      title="Посмотрите слабые вопросы"
                      text="Смотрите, какие вопросы вызывают наибольшее число ошибок и требуют доработки."
                    />
                    <GuideStep
                      index="03"
                      title="Поддерживайте качество учебного содержания"
                      text="Обновляйте материалы и тесты на основе накопленной статистики прохождений."
                    />
                  </>
                ) : (
                  <>
                    <GuideStep
                      index="01"
                      title="Проверьте обязательные модули"
                      text="Активные назначения и сроки собраны в каталоге обучения."
                    />
                    <GuideStep
                      index="02"
                      title="Добавьте самостоятельную практику"
                      text="Откройте модуль для свободного прохождения и закрепите материал."
                    />
                    <GuideStep
                      index="03"
                      title="Посмотрите итог в результатах"
                      text="История завершённых попыток уже поддерживает детальный экран результата."
                    />
                  </>
                )}
              </div>
            </Card>
          </div>
        </Col>

        <Col xs={24} lg={12}>
          <div className="home-side-stack">
            {!hasExpertArea ? (
              <Card className="home-notification-card">
                <div className="home-notification-header">
                  <Typography.Title level={3} className="home-section-title">
                    Последние уведомления
                  </Typography.Title>
                  {recentNotifications.length > 0 ? (
                    <Link to="/learner/notifications">
                      <Button type="link">Все уведомления</Button>
                    </Link>
                  ) : null}
                </div>
                <div className="home-notification-list">
                  {recentNotifications.length > 0 ? (
                    recentNotifications.map((notification) => (
                      <div key={notification.id} className="home-notification-item">
                        <Link to={`/learner/notifications/${notification.id}`} className="home-notification-link">
                          {notification.title || 'Уведомление'}
                        </Link>
                        {notification.message ? (
                          <Typography.Paragraph className="home-note-text">
                            {notification.message}
                          </Typography.Paragraph>
                        ) : null}
                        <Typography.Text type="secondary" className="home-notification-item-date">
                          {formatUiDate(notification.createdAt ?? undefined)}
                        </Typography.Text>
                      </div>
                    ))
                  ) : (
                    <Typography.Paragraph className="home-note-text">
                      Пока новых уведомлений нет.
                    </Typography.Paragraph>
                  )}
                </div>
              </Card>
            ) : null}
          </div>
        </Col>
      </Row>
    </div>
  );
}

function SummaryMetricCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="home-summary-card">
      <span className="home-summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function GuideStep({ index, title, text }: { index: string; title: string; text: string }) {
  return (
    <div className="home-guide-item">
      <span className="home-guide-index">{index}</span>
      <div>
        <Typography.Text strong>{title}</Typography.Text>
        <Typography.Paragraph className="home-note-text">{text}</Typography.Paragraph>
      </div>
    </div>
  );
}

function AdminHubLink({
  kicker,
  title,
  description,
  to,
}: {
  kicker: string;
  title: string;
  description: string;
  to: string;
}) {
  return (
    <Link to={to} className="home-admin-link">
      <span className="home-admin-link-kicker">{kicker}</span>
      <span className="home-admin-link-title">{title}</span>
      <Typography.Paragraph className="home-admin-link-text">{description}</Typography.Paragraph>
    </Link>
  );
}

function toPercentNumber(value?: string | number | null): number {
  if (value == null) {
    return 0;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function compareQueueItems(left: ManagerQueueItem, right: ManagerQueueItem): number {
  if (!left.deadlineAt && !right.deadlineAt) {
    return left.employeeName.localeCompare(right.employeeName);
  }
  if (!left.deadlineAt) {
    return 1;
  }
  if (!right.deadlineAt) {
    return -1;
  }
  return new Date(left.deadlineAt).getTime() - new Date(right.deadlineAt).getTime();
}

function localizeManagerAssignmentStatus(status?: string): string {
  switch (status) {
    case 'OVERDUE':
      return 'Просрочено';
    case 'IN_PROGRESS':
      return 'В работе';
    case 'ACTIVE':
      return 'Активно';
    case 'ASSIGNED':
      return 'Назначено';
    case 'COMPLETED':
      return 'Завершено';
    default:
      return status ?? 'Не указано';
  }
}
