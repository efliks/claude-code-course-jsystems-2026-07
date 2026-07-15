package com.jsystems.hsdc.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers ADR-001 §3 "PolicyService" and TAC-001-04 (fail-fast startup when a
 * policy file is missing). {@link PolicyService#loadPolicies()} is invoked
 * directly rather than via a Spring context, since these are narrow unit
 * tests, not a slice test — {@code @PostConstruct} triggers the same method
 * when the bean is actually built by Spring.
 */
class PolicyServiceTest {

    @Test
    void loadsBothPolicyFilesAndExposesTheirContent(@TempDir Path dir) throws IOException {
        writeFile(dir, "return-policy.md", "# Return Policy\ncontent");
        writeFile(dir, "complaint-policy.md", "# Complaint Policy\ncontent");

        PolicyService service = new PolicyService(dir.toString());
        service.loadPolicies();

        assertThat(service.returnPolicy()).isEqualTo("# Return Policy\ncontent");
        assertThat(service.complaintPolicy()).isEqualTo("# Complaint Policy\ncontent");
    }

    @Test
    void cachesContentSoItIsStillServedAfterTheFileIsDeleted(@TempDir Path dir) throws IOException {
        writeFile(dir, "return-policy.md", "cached return content");
        writeFile(dir, "complaint-policy.md", "cached complaint content");

        PolicyService service = new PolicyService(dir.toString());
        service.loadPolicies();

        // Prove the getters serve the in-memory cache, not the disk: deleting
        // the source files must not affect already-loaded content.
        Files.delete(dir.resolve("return-policy.md"));
        Files.delete(dir.resolve("complaint-policy.md"));

        assertThat(service.returnPolicy()).isEqualTo("cached return content");
        assertThat(service.complaintPolicy()).isEqualTo("cached complaint content");
    }

    @Test
    void missingReturnPolicyFileFailsFastWithAReadableMessage(@TempDir Path dir) throws IOException {
        writeFile(dir, "complaint-policy.md", "content");

        PolicyService service = new PolicyService(dir.toString());

        assertThatThrownBy(service::loadPolicies)
                .isInstanceOf(PolicyLoadException.class)
                .hasMessageContaining("return-policy.md");
    }

    @Test
    void missingComplaintPolicyFileFailsFastWithAReadableMessage(@TempDir Path dir) throws IOException {
        writeFile(dir, "return-policy.md", "content");

        PolicyService service = new PolicyService(dir.toString());

        assertThatThrownBy(service::loadPolicies)
                .isInstanceOf(PolicyLoadException.class)
                .hasMessageContaining("complaint-policy.md");
    }

    @Test
    void missingPoliciesDirectoryFailsFastWithAReadableMessage(@TempDir Path dir) {
        Path missingDir = dir.resolve("does-not-exist");

        PolicyService service = new PolicyService(missingDir.toString());

        assertThatThrownBy(service::loadPolicies)
                .isInstanceOf(PolicyLoadException.class)
                .hasMessageContaining("return-policy.md");
    }

    private static void writeFile(Path dir, String filename, String content) throws IOException {
        Files.writeString(dir.resolve(filename), content);
    }
}
