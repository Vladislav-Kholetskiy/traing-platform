import { Navigate, useParams } from 'react-router';

export function AssignmentDetailPage() {
  const { assignmentId } = useParams();

  if (!assignmentId) {
    return <Navigate replace to="/learner/assigned-learning" />;
  }

  return <Navigate replace to={`/learner/assigned-learning/${assignmentId}/learning-context`} />;
}
