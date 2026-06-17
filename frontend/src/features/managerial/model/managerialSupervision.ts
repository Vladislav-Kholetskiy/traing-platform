export type ManagerialCurrentSupervisionDto = {
  assignmentId?: number | null;
  userId?: number | null;
  userDisplayName?: string | null;
  courseId?: number | null;
  courseName?: string | null;
  assignmentTestCount?: number | null;
  assignedAt?: string | null;
  deadlineAt?: string | null;
  assignmentStatus?: string | null;
};

export type ManagerialCurrentSupervisionItem = {
  assignmentId?: number;
  userId?: number;
  userDisplayName?: string;
  courseId?: number;
  courseName?: string;
  assignmentTestCount?: number;
  assignedAt?: string;
  deadlineAt?: string;
  assignmentStatus?: string;
};

export function mapManagerialCurrentSupervisionItem(
  dto: ManagerialCurrentSupervisionDto,
): ManagerialCurrentSupervisionItem {
  return {
    assignmentId: dto.assignmentId ?? undefined,
    userId: dto.userId ?? undefined,
    userDisplayName: dto.userDisplayName ?? undefined,
    courseId: dto.courseId ?? undefined,
    courseName: dto.courseName ?? undefined,
    assignmentTestCount: dto.assignmentTestCount ?? undefined,
    assignedAt: dto.assignedAt ?? undefined,
    deadlineAt: dto.deadlineAt ?? undefined,
    assignmentStatus: dto.assignmentStatus ?? undefined,
  };
}
