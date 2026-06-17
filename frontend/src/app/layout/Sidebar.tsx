import { Badge, Menu, Space, Typography } from 'antd';
import type { ItemType } from 'antd/es/menu/interface';
import { Link, useLocation } from 'react-router';
import {
  canAccessAdministrationArea,
  canAccessAssignmentCampaignArea,
  canAccessExpertArea,
  canAccessManagerArea,
  type CurrentActor,
} from '../../features/auth/model/currentActor';
import { DemoActorSwitcher } from '../../features/auth/ui/DemoActorSwitcher';

type SidebarProps = {
  actor: CurrentActor;
};

function getSelectedKey(pathname: string): string {
  const knownKeys = [
    '/learner/notifications',
    '/learner/self-results',
    '/learner/self-testing',
    '/learner/assigned-learning',
    '/learner/learning',
    '/manager/current-supervision',
    '/manager/analytics/user-topic',
    '/manager/analytics/topics',
    '/manager/analytics/department-topic',
    '/expert/content/courses',
    '/expert/analytics/questions',
    '/admin/users',
    '/admin/organization',
    '/admin/access',
    '/admin/assignment-campaigns',
    '/admin/assignment-individual',
    '/admin/assignment-service',
    '/admin/notifications',
    '/admin/import/jobs',
    '/admin/audit',
  ];

  return knownKeys.find((key) => pathname.startsWith(key)) ?? '/';
}

export function Sidebar({ actor }: SidebarProps) {
  const location = useLocation();
  const canViewAssigned = actor.enabledSections.includes('ASSIGNED_LEARNING');
  const canViewSelfTesting = actor.enabledSections.includes('SELF_TESTING');
  const canViewResults = actor.enabledSections.includes('SELF_RESULTS');
  const canViewLearningCatalog = canViewAssigned || canViewSelfTesting;
  const canViewManager = canAccessManagerArea(actor);
  const canViewExpert = canAccessExpertArea(actor);
  const canViewAssignmentCampaigns = canAccessAssignmentCampaignArea(actor);
  const canViewAdministration = canAccessAdministrationArea(actor);

  const assignmentItems: ItemType[] = canViewAssignmentCampaigns
    ? [
        {
          key: '/admin/assignment-campaigns',
          label: <Link to="/admin/assignment-campaigns">Кампании назначений</Link>,
        },
        {
          key: '/admin/assignment-individual',
          label: <Link to="/admin/assignment-individual">Индивидуальные назначения</Link>,
        },
        {
          key: '/admin/assignment-service',
          label: <Link to="/admin/assignment-service">Служебные действия</Link>,
        },
      ]
    : [];

  const adminItems: ItemType[] = canViewAdministration
    ? [
        {
          type: 'group',
          label: 'Основное',
          children: [
            { key: '/admin/users', label: <Link to="/admin/users">Пользователи</Link> },
            { key: '/admin/organization', label: <Link to="/admin/organization">Оргструктура</Link> },
            { key: '/admin/access', label: <Link to="/admin/access">Доступы</Link> },
          ],
        },
        ...(assignmentItems.length
          ? [
              {
                type: 'group',
                label: 'Назначения',
                children: assignmentItems,
              } satisfies ItemType,
            ]
          : []),
        {
          type: 'group',
          label: 'Операции',
          children: [
            { key: '/admin/notifications', label: <Link to="/admin/notifications">Уведомления</Link> },
            { key: '/admin/import/jobs', label: <Link to="/admin/import/jobs">Импорт</Link> },
            { key: '/admin/audit', label: <Link to="/admin/audit">Аудит</Link> },
          ],
        },
      ]
    : [];

  const nonAdminItems: ItemType[] = canViewAdministration
    ? []
    : [
        ...(canViewLearningCatalog
          ? [{ key: '/learner/learning', label: <Link to="/learner/learning">Обучение</Link> } satisfies ItemType]
          : []),
        ...(canViewAssigned
          ? [
              {
                key: '/learner/assigned-learning',
                label: <Link to="/learner/assigned-learning">Назначенное обучение</Link>,
              } satisfies ItemType,
            ]
          : []),
        ...(canViewSelfTesting
          ? [
              {
                key: '/learner/self-testing',
                label: <Link to="/learner/self-testing">Самостоятельное обучение</Link>,
              } satisfies ItemType,
            ]
          : []),
        ...(canViewResults
          ? [{ key: '/learner/self-results', label: <Link to="/learner/self-results">Результаты</Link> } satisfies ItemType]
          : []),
        { key: '/learner/notifications', label: <Link to="/learner/notifications">Уведомления</Link> },
        ...(canViewManager
          ? [
              {
                key: '/manager/current-supervision',
                label: <Link to="/manager/current-supervision">Контроль обучения</Link>,
              } satisfies ItemType,
              {
                key: '/manager/analytics/user-topic',
                label: <Link to="/manager/analytics/user-topic">Аналитика по сотрудникам</Link>,
              } satisfies ItemType,
              {
                key: '/manager/analytics/topics',
                label: <Link to="/manager/analytics/topics">Аналитика по темам</Link>,
              } satisfies ItemType,
              {
                key: '/manager/analytics/department-topic',
                label: <Link to="/manager/analytics/department-topic">Аналитика по подразделениям</Link>,
              } satisfies ItemType,
            ]
          : []),
        ...(canViewExpert
          ? [
              {
                key: '/expert/content/courses',
                label: <Link to="/expert/content/courses">Учебные материалы</Link>,
              } satisfies ItemType,
              {
                key: '/expert/analytics/questions',
                label: <Link to="/expert/analytics/questions">Аналитика</Link>,
              } satisfies ItemType,
              ...assignmentItems,
            ]
          : []),
      ];

  const activeItems: ItemType[] = [
    { key: '/', label: <Link to="/">Главная</Link> },
    ...nonAdminItems,
    ...adminItems,
  ];

  return (
    <div className="sidebar-shell">
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Badge color="#1677ff" />
        <Typography.Title level={4} style={{ margin: 0 }}>
          Training Platform
        </Typography.Title>
      </Space>

      <div className="sidebar-section">
        <Typography.Text strong>Доступные разделы</Typography.Text>
      </div>
      <Menu
        mode="inline"
        selectedKeys={[getSelectedKey(location.pathname)]}
        items={activeItems}
        style={{ borderInlineEnd: 'none', background: 'transparent' }}
        className="sidebar-menu"
      />

      <DemoActorSwitcher actor={actor} />
    </div>
  );
}
