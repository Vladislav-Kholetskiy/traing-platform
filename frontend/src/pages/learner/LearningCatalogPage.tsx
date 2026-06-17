import { Col, Row, Typography } from 'antd';
import { AssignedLearningList } from '../../features/assigned-learning/ui/AssignedLearningList';
import { hasSection } from '../../features/auth/model/currentActor';
import { useCurrentActor } from '../../features/auth/model/useCurrentActor';
import { useAssignedLearningList } from '../../features/assigned-learning/model/useAssignedLearning';
import { useSelfTestingCatalog } from '../../features/self-testing/model/useSelfTesting';
import { SelfTestingCatalog } from '../../features/self-testing/ui/SelfTestingCatalog';
import { hasErrorStatus } from '../../shared/api/apiError';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorView } from '../../shared/ui/ErrorView';
import { ForbiddenView } from '../../shared/ui/ForbiddenView';
import { LoadingView } from '../../shared/ui/LoadingView';

const UI_TEXT = {
  loadingTitle: '\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0430 \u043e\u0431\u0443\u0447\u0435\u043d\u0438\u044f',
  loadingDescription:
    '\u0421\u043e\u0431\u0438\u0440\u0430\u0435\u043c \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u043d\u044b\u0435 \u0438 \u0441\u0430\u043c\u043e\u0441\u0442\u043e\u044f\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043c\u043e\u0434\u0443\u043b\u0438 \u0432 \u0435\u0434\u0438\u043d\u0443\u044e \u0432\u0438\u0442\u0440\u0438\u043d\u0443.',
  assignedLoadError: '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u043d\u043e\u0435 \u043e\u0431\u0443\u0447\u0435\u043d\u0438\u0435',
  selfLoadError: '\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0441\u0430\u043c\u043e\u0441\u0442\u043e\u044f\u0442\u0435\u043b\u044c\u043d\u043e\u0435 \u043e\u0431\u0443\u0447\u0435\u043d\u0438\u0435',
  retryLater:
    '\u041f\u043e\u043f\u0440\u043e\u0431\u0443\u0439\u0442\u0435 \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043a\u0430\u0442\u0430\u043b\u043e\u0433 \u0435\u0449\u0451 \u0440\u0430\u0437 \u043d\u0435\u043c\u043d\u043e\u0433\u043e \u043f\u043e\u0437\u0436\u0435.',
  catalogTitle: '\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u043e\u0431\u0443\u0447\u0435\u043d\u0438\u044f',
  emptyCatalog: '\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u043f\u043e\u043a\u0430 \u043f\u0443\u0441\u0442',
  emptyCatalogDescription:
    '\u041a\u043e\u0433\u0434\u0430 \u043f\u043e\u044f\u0432\u044f\u0442\u0441\u044f \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u043d\u044b\u0435 \u0438\u043b\u0438 \u0441\u0430\u043c\u043e\u0441\u0442\u043e\u044f\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043c\u043e\u0434\u0443\u043b\u0438, \u043e\u043d\u0438 \u043e\u0442\u043e\u0431\u0440\u0430\u0437\u044f\u0442\u0441\u044f \u043d\u0430 \u044d\u0442\u043e\u0439 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0435.',
  assignedTitle: '\u041d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u043d\u043e\u0435 \u043e\u0431\u0443\u0447\u0435\u043d\u0438\u0435',
  assignedDescription: '\u041e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043c\u043e\u0434\u0443\u043b\u0438 \u0438 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0435 \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u044f.',
  noAssignments: '\u041d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0439',
  noAssignmentsDescription:
    '\u041a\u043e\u0433\u0434\u0430 \u043f\u043e\u044f\u0432\u044f\u0442\u0441\u044f \u043e\u0431\u044f\u0437\u0430\u0442\u0435\u043b\u044c\u043d\u044b\u0435 \u043c\u043e\u0434\u0443\u043b\u0438, \u043e\u043d\u0438 \u0431\u0443\u0434\u0443\u0442 \u043f\u043e\u043a\u0430\u0437\u0430\u043d\u044b \u0432 \u044d\u0442\u043e\u043c \u0431\u043b\u043e\u043a\u0435.',
  selfTitle: '\u0421\u0430\u043c\u043e\u0441\u0442\u043e\u044f\u0442\u0435\u043b\u044c\u043d\u043e\u0435 \u043e\u0431\u0443\u0447\u0435\u043d\u0438\u0435',
  selfDescription:
    '\u041c\u043e\u0434\u0443\u043b\u0438 \u0434\u043b\u044f \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u043e\u0433\u043e \u043f\u0440\u043e\u0445\u043e\u0436\u0434\u0435\u043d\u0438\u044f \u0438 \u0442\u0440\u0435\u043d\u0438\u0440\u043e\u0432\u043a\u0438.',
  noSelfModules: '\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0441\u0430\u043c\u043e\u0441\u0442\u043e\u044f\u0442\u0435\u043b\u044c\u043d\u044b\u0445 \u043c\u043e\u0434\u0443\u043b\u0435\u0439',
  noSelfModulesDescription:
    '\u041a\u043e\u0433\u0434\u0430 \u043a\u0430\u0442\u0430\u043b\u043e\u0433 \u043f\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u0441\u044f, \u043d\u043e\u0432\u044b\u0435 \u044d\u043b\u0435\u043c\u0435\u043d\u0442\u044b \u043f\u043e\u044f\u0432\u044f\u0442\u0441\u044f \u0437\u0434\u0435\u0441\u044c.',
};

export function LearningCatalogPage() {
  const { data: actor } = useCurrentActor();
  const canViewAssigned = Boolean(actor && hasSection(actor, 'ASSIGNED_LEARNING'));
  const canViewSelf = Boolean(actor && hasSection(actor, 'SELF_TESTING'));
  const assignedQuery = useAssignedLearningList(canViewAssigned);
  const selfTestingQuery = useSelfTestingCatalog(canViewSelf);

  if (!actor || (!canViewAssigned && !canViewSelf)) {
    return <ForbiddenView />;
  }

  if ((canViewAssigned && assignedQuery.isLoading) || (canViewSelf && selfTestingQuery.isLoading)) {
    return <LoadingView title={UI_TEXT.loadingTitle} description={UI_TEXT.loadingDescription} />;
  }

  if (canViewAssigned && assignedQuery.isError && !hasErrorStatus(assignedQuery.error, 403)) {
    return (
      <ErrorView
        title={UI_TEXT.assignedLoadError}
        description={UI_TEXT.retryLater}
        error={assignedQuery.error}
      />
    );
  }

  if (canViewSelf && selfTestingQuery.isError && !hasErrorStatus(selfTestingQuery.error, 403)) {
    return (
      <ErrorView
        title={UI_TEXT.selfLoadError}
        description={UI_TEXT.retryLater}
        error={selfTestingQuery.error}
      />
    );
  }

  const assignedItems = canViewAssigned ? assignedQuery.data ?? [] : [];
  const selfItems = canViewSelf ? selfTestingQuery.data ?? [] : [];

  if (assignedItems.length === 0 && selfItems.length === 0) {
    return (
      <>
        <Typography.Title level={2}>{UI_TEXT.catalogTitle}</Typography.Title>
        <EmptyState title={UI_TEXT.emptyCatalog} description={UI_TEXT.emptyCatalogDescription} />
      </>
    );
  }

  return (
    <div className="home-page home-page-compact">
      <Row gutter={[20, 20]}>
        {canViewAssigned ? (
          <Col xs={24}>
            <div className="page-header page-header-tight">
              <div>
                <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 8 }}>
                  {UI_TEXT.assignedTitle}
                </Typography.Title>
                <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  {UI_TEXT.assignedDescription}
                </Typography.Paragraph>
              </div>
            </div>
            {assignedItems.length > 0 ? (
              <AssignedLearningList assignments={assignedItems} />
            ) : (
              <EmptyState title={UI_TEXT.noAssignments} description={UI_TEXT.noAssignmentsDescription} />
            )}
          </Col>
        ) : null}

        {canViewSelf ? (
          <Col xs={24}>
            <div className="page-header page-header-tight">
              <div>
                <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 8 }}>
                  {UI_TEXT.selfTitle}
                </Typography.Title>
                <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  {UI_TEXT.selfDescription}
                </Typography.Paragraph>
              </div>
            </div>
            {selfItems.length > 0 ? (
              <SelfTestingCatalog tests={selfItems} />
            ) : (
              <EmptyState title={UI_TEXT.noSelfModules} description={UI_TEXT.noSelfModulesDescription} />
            )}
          </Col>
        ) : null}
      </Row>
    </div>
  );
}
