package com.vladislav.training.platform.userorg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnit;
import com.vladislav.training.platform.userorg.domain.OrganizationalUnitStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/**
 * Проверяет вспомогательную логику {@code OrganizationalUnitTree}.
 * Хотя это не центральный компонент, от него зависит предсказуемость работы.
 */
class OrganizationalUnitTreeSupportTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-28T12:00:00Z");

    private final OrganizationalUnitTreePathBuilder pathBuilder = new OrganizationalUnitTreePathBuilder();
    private final OrganizationalUnitSubtreeRebuilder subtreeRebuilder = new OrganizationalUnitSubtreeRebuilder(pathBuilder);
    private final OrganizationalUnitMoveValidator moveValidator = new OrganizationalUnitMoveValidator();

    @Test
    void resolvePlacementBuildsCanonicalRootPathAndDepth() {
        OrganizationalUnitTreePosition position = pathBuilder.resolvePlacement(null, " Root Team ");

        assertThat(position.path()).isEqualTo("/root-team");
        assertThat(position.depth()).isZero();
    }

    @Test
    void rebuildSubtreeRecalculatesDescendantPathAndDepth() {
        OrganizationalUnit rebuiltRoot = unit(2L, 1L, "Renamed Branch", "/root/renamed-branch", 1);
        OrganizationalUnit child = unit(3L, 2L, "Leaf", "/root/branch/leaf", 2);
        OrganizationalUnit grandChild = unit(4L, 3L, "Deep Node", "/root/branch/leaf/deep-node", 3);

        List<OrganizationalUnit> rebuiltSubtree = subtreeRebuilder.rebuildSubtree(
            rebuiltRoot,
            Map.of(2L, List.of(child), 3L, List.of(grandChild), 4L, List.of()),
            FIXED_INSTANT
        );

        assertThat(rebuiltSubtree)
            .extracting(OrganizationalUnit::path)
            .containsExactly(
                "/root/renamed-branch",
                "/root/renamed-branch/leaf",
                "/root/renamed-branch/leaf/deep-node"
            );
        assertThat(rebuiltSubtree)
            .extracting(OrganizationalUnit::depth)
            .containsExactly(1, 2, 3);
    }

    @Test
    void moveValidatorRejectsCycleAgainstCurrentSubtreeSnapshot() {
        OrganizationalUnit currentUnit = unit(2L, 1L, "Branch", "/root/branch", 1);
        OrganizationalUnit descendant = unit(3L, 2L, "Leaf", "/root/branch/leaf", 2);

        assertThatThrownBy(() -> moveValidator.ensureMoveDoesNotCreateCycle(
            currentUnit,
            descendant,
            Map.of(2L, List.of(descendant), 3L, List.of())
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("create a cycle");
    }

    private OrganizationalUnit unit(Long id, Long parentId, String name, String path, int depth) {
        return new OrganizationalUnit(
            id,
            parentId,
            10L,
            name,
            OrganizationalUnitStatus.ACTIVE,
            path,
            depth,
            null,
            FIXED_INSTANT,
            FIXED_INSTANT
        );
    }
}
