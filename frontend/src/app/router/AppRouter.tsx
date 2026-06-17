import { Navigate, Route, Routes } from 'react-router';
import { MainLayout } from '../layout/MainLayout';
import { HomePage } from '../../pages/HomePage';
import { AssignedLearningListPage } from '../../pages/learner/AssignedLearningListPage';
import { AssignmentDetailPage } from '../../pages/learner/AssignmentDetailPage';
import { AssignmentLearningContextPage } from '../../pages/learner/AssignmentLearningContextPage';
import { AssignedMaterialContentPage } from '../../pages/learner/AssignedMaterialContentPage';
import { AssignedTestContextPage } from '../../pages/learner/AssignedTestContextPage';
import { AssignedAttemptPage } from '../../pages/learner/AssignedAttemptPage';
import { SelfResultsPage } from '../../pages/learner/SelfResultsPage';
import { SelfTestingListPage } from '../../pages/learner/SelfTestingListPage';
import { SelfTopicDetailPage } from '../../pages/learner/SelfTopicDetailPage';
import { SelfTestDetailPage } from '../../pages/learner/SelfTestDetailPage';
import { SelfAttemptPage } from '../../pages/learner/SelfAttemptPage';
import { SelfResultDetailPage } from '../../pages/learner/SelfResultDetailPage';
import { LearningCatalogPage } from '../../pages/learner/LearningCatalogPage';
import { ManagerCurrentSupervisionPage } from '../../pages/manager/ManagerCurrentSupervisionPage';
import { ManagerUserTopicAnalyticsPage } from '../../pages/manager/ManagerUserTopicAnalyticsPage';
import { ManagerDepartmentTopicAnalyticsPage } from '../../pages/manager/ManagerDepartmentTopicAnalyticsPage';
import { ManagerTopicAnalyticsPage } from '../../pages/manager/ManagerTopicAnalyticsPage';
import { AdminUsersPage } from '../../pages/admin/AdminUsersPage';
import { AdminUserDetailPage } from '../../pages/admin/AdminUserDetailPage';
import { AdminOrganizationPage } from '../../pages/admin/AdminOrganizationPage';
import { AdminOrganizationUnitPage } from '../../pages/admin/AdminOrganizationUnitPage';
import { AdminAccessManagementPage } from '../../pages/admin/AdminAccessManagementPage';
import { AdminImportJobsPage } from '../../pages/admin/AdminImportJobsPage';
import { AdminImportJobDetailPage } from '../../pages/admin/AdminImportJobDetailPage';
import { AdminImportItemDetailPage } from '../../pages/admin/AdminImportItemDetailPage';
import { AdminAuditPage } from '../../pages/admin/AdminAuditPage';
import { AdminAuditDetailPage } from '../../pages/admin/AdminAuditDetailPage';
import { AdminAnalyticsRebuildPage } from '../../pages/admin/AdminAnalyticsRebuildPage';
import { AdminAssignmentCampaignPage } from '../../pages/admin/AdminAssignmentCampaignPage';
import { ExpertCoursesPage } from '../../pages/expert/ExpertCoursesPage';
import { ExpertCourseDetailPage } from '../../pages/expert/ExpertCourseDetailPage';
import { ExpertTopicDetailPage } from '../../pages/expert/ExpertTopicDetailPage';
import { ExpertTopicMaterialsPage } from '../../pages/expert/ExpertTopicMaterialsPage';
import { ExpertTopicQuestionsPage } from '../../pages/expert/ExpertTopicQuestionsPage';
import { ExpertQuestionDetailPage } from '../../pages/expert/ExpertQuestionDetailPage';
import { ExpertTopicTestsPage } from '../../pages/expert/ExpertTopicTestsPage';
import { ExpertTestDetailPage } from '../../pages/expert/ExpertTestDetailPage';
import { ExpertTopicFinalControlPage } from '../../pages/expert/ExpertTopicFinalControlPage';
import { ExpertQuestionAnalyticsPage } from '../../pages/expert/ExpertQuestionAnalyticsPage';
import { SelfNotificationsPage } from '../../pages/notifications/SelfNotificationsPage';
import { SelfNotificationDetailPage } from '../../pages/notifications/SelfNotificationDetailPage';
import { AdminNotificationsPage } from '../../pages/notifications/AdminNotificationsPage';
import { AdminNotificationDetailPage } from '../../pages/notifications/AdminNotificationDetailPage';

export function AppRouter() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="/learner/learning" element={<LearningCatalogPage />} />
        <Route path="/learner/assigned-learning" element={<AssignedLearningListPage />} />
        <Route path="/learner/assigned-learning/:assignmentId" element={<AssignmentDetailPage />} />
        <Route
          path="/learner/assigned-learning/:assignmentId/learning-context"
          element={<AssignmentLearningContextPage />}
        />
        <Route
          path="/learner/assigned-learning/:assignmentId/materials/:materialId"
          element={<AssignedMaterialContentPage />}
        />
        <Route
          path="/learner/assigned-learning/:assignmentId/tests/:assignmentTestId"
          element={<AssignedTestContextPage />}
        />
        <Route
          path="/learner/assigned-learning/:assignmentId/tests/:assignmentTestId/attempt"
          element={<AssignedAttemptPage />}
        />
        <Route path="/learner/self-testing" element={<SelfTestingListPage />} />
        <Route path="/learner/self-testing/topics/:topicId" element={<SelfTopicDetailPage />} />
        <Route path="/learner/self-testing/:testId" element={<SelfTestDetailPage />} />
        <Route path="/learner/self-testing/:testId/attempt" element={<SelfAttemptPage />} />
        <Route path="/learner/self-results" element={<SelfResultsPage />} />
        <Route path="/learner/self-results/:resultId" element={<SelfResultDetailPage />} />
        <Route path="/learner/notifications" element={<SelfNotificationsPage />} />
        <Route path="/learner/notifications/:notificationId" element={<SelfNotificationDetailPage />} />
        <Route path="/notifications/self" element={<Navigate replace to="/learner/notifications" />} />
        <Route path="/notifications/self/:notificationId" element={<SelfNotificationDetailPage />} />
        <Route path="/manager/current-supervision" element={<ManagerCurrentSupervisionPage />} />
        <Route path="/manager/analytics/user-topic" element={<ManagerUserTopicAnalyticsPage />} />
        <Route path="/manager/analytics/topics" element={<ManagerTopicAnalyticsPage />} />
        <Route
          path="/manager/analytics/department-topic"
          element={<ManagerDepartmentTopicAnalyticsPage />}
        />
        <Route path="/expert/content/courses" element={<ExpertCoursesPage />} />
        <Route path="/expert/content/courses/:courseId" element={<ExpertCourseDetailPage />} />
        <Route path="/expert/content/courses/:courseId/topics" element={<ExpertCourseDetailPage />} />
        <Route path="/expert/content/topics/:topicId" element={<ExpertTopicDetailPage />} />
        <Route path="/expert/content/topics/:topicId/materials" element={<ExpertTopicMaterialsPage />} />
        <Route path="/expert/content/topics/:topicId/questions" element={<ExpertTopicQuestionsPage />} />
        <Route path="/expert/content/questions/:questionId" element={<ExpertQuestionDetailPage />} />
        <Route path="/expert/content/topics/:topicId/tests" element={<ExpertTopicTestsPage />} />
        <Route path="/expert/content/tests/:testId" element={<ExpertTestDetailPage />} />
        <Route path="/expert/content/topics/:topicId/final-control" element={<ExpertTopicFinalControlPage />} />
        <Route path="/expert/analytics/questions" element={<ExpertQuestionAnalyticsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/admin/users/:userId" element={<AdminUserDetailPage />} />
        <Route path="/admin/organization" element={<AdminOrganizationPage />} />
        <Route path="/admin/organization/:unitId" element={<AdminOrganizationUnitPage />} />
        <Route path="/admin/access" element={<AdminAccessManagementPage />} />
        <Route path="/admin/assignment-campaigns" element={<AdminAssignmentCampaignPage />} />
        <Route path="/admin/assignment-individual" element={<AdminAssignmentCampaignPage />} />
        <Route path="/admin/assignment-service" element={<AdminAssignmentCampaignPage />} />
        <Route path="/admin/notifications" element={<AdminNotificationsPage />} />
        <Route path="/admin/notifications/:notificationId" element={<AdminNotificationDetailPage />} />
        <Route path="/admin/import/jobs" element={<AdminImportJobsPage />} />
        <Route path="/admin/import/jobs/:importJobId" element={<AdminImportJobDetailPage />} />
        <Route path="/admin/import/items/:itemId" element={<AdminImportItemDetailPage />} />
        <Route path="/admin/audit" element={<AdminAuditPage />} />
        <Route path="/admin/audit/:auditEventId" element={<AdminAuditDetailPage />} />
        <Route path="/admin/analytics/rebuild" element={<AdminAnalyticsRebuildPage />} />
      </Route>
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  );
}
