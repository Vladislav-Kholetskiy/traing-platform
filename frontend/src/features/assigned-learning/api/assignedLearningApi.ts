import { apiClient } from '../../../shared/api/apiClient';
import {
  mapAssignedLearningAssignment,
  mapAssignedLearningContext,
  mapAssignedMaterialContent,
  mapAssignedTestContext,
  type AssignedMaterialContent,
  type AssignedMaterialContentDto,
  type AssignedLearningAssignment,
  type AssignedLearningAssignmentDto,
  type AssignedLearningContext,
  type AssignedLearningContextDto,
  type AssignedTestContext,
  type AssignedTestContextDto,
} from '../model/assignedLearning';

export async function getAssignedLearningList(): Promise<AssignedLearningAssignment[]> {
  const response = await apiClient.get<AssignedLearningAssignmentDto[]>(
    '/api/v1/assigned-learning/assignments',
  );

  return Array.isArray(response)
    ? response
      .map(mapAssignedLearningAssignment)
      .filter((assignment): assignment is AssignedLearningAssignment & { assignmentId: number } => assignment.assignmentId != null)
    : [];
}

export async function getAssignmentDetail(
  assignmentId: string | number,
): Promise<AssignedLearningAssignment> {
  const response = await apiClient.get<AssignedLearningAssignmentDto>(
    `/api/v1/assigned-learning/assignments/${assignmentId}`,
  );

  return mapAssignedLearningAssignment(response);
}

export async function getAssignmentLearningContext(
  assignmentId: string | number,
): Promise<AssignedLearningContext> {
  const response = await apiClient.get<AssignedLearningContextDto>(
    `/api/v1/assigned-learning/assignments/${assignmentId}/learning-context`,
  );

  return mapAssignedLearningContext(response);
}

export async function getAssignedTestContext(
  assignmentId: string | number,
  assignmentTestId: string | number,
): Promise<AssignedTestContext> {
  const response = await apiClient.get<AssignedTestContextDto>(
    `/api/v1/assigned-learning/assignments/${assignmentId}/assignment-tests/${assignmentTestId}/test-context`,
  );

  return mapAssignedTestContext(response);
}

export async function getAssignedMaterialContent(
  assignmentId: string | number,
  materialId: string | number,
): Promise<AssignedMaterialContent> {
  const response = await apiClient.get<AssignedMaterialContentDto>(
    `/api/v1/assigned-learning/assignments/${assignmentId}/materials/${materialId}`,
  );

  return mapAssignedMaterialContent(response);
}
