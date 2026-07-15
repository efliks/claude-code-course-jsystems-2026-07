package com.jsystems.hsdc.policy;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Loads and caches the two policy markdown documents (ADR-001 §3
 * "PolicyService"; ADR-000 §7 {@code HSDC_POLICIES_DIR}).
 *
 * <p>{@link #loadPolicies()} runs once at startup ({@code @PostConstruct}),
 * reading {@value #RETURN_POLICY_FILENAME} and
 * {@value #COMPLAINT_POLICY_FILENAME} from the configured directory into
 * memory; {@link #returnPolicy()} and {@link #complaintPolicy()} then serve
 * the cached text without touching the disk again, since the content is
 * later injected verbatim into LLM prompts on every request.
 *
 * <p>If either file is missing or unreadable, {@link #loadPolicies()} throws
 * {@link PolicyLoadException} naming the exact path. Spring wraps this into
 * a {@code BeanCreationException} that aborts application startup (TAC-001-04:
 * "no half-working state" — the app must not start able to decide cases
 * against a partially-loaded policy).
 */
@Service
public class PolicyService {

    static final String RETURN_POLICY_FILENAME = "return-policy.md";
    static final String COMPLAINT_POLICY_FILENAME = "complaint-policy.md";

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final Path policiesDir;

    private String returnPolicy;
    private String complaintPolicy;

    public PolicyService(@Value("${HSDC_POLICIES_DIR:../../docs/policies}") String policiesDir) {
        this.policiesDir = Path.of(policiesDir);
    }

    @PostConstruct
    public void loadPolicies() {
        returnPolicy = readPolicyFile(RETURN_POLICY_FILENAME);
        complaintPolicy = readPolicyFile(COMPLAINT_POLICY_FILENAME);
        log.info("Loaded policy documents from {}", policiesDir.toAbsolutePath());
    }

    /** Full markdown text of the return policy, cached at startup. */
    public String returnPolicy() {
        return returnPolicy;
    }

    /** Full markdown text of the complaint policy, cached at startup. */
    public String complaintPolicy() {
        return complaintPolicy;
    }

    private String readPolicyFile(String filename) {
        Path path = policiesDir.resolve(filename).toAbsolutePath();
        if (!Files.isRegularFile(path)) {
            throw new PolicyLoadException(
                    "Required policy document not found: " + path
                            + ". Configure HSDC_POLICIES_DIR to point at a directory containing "
                            + RETURN_POLICY_FILENAME + " and " + COMPLAINT_POLICY_FILENAME + ".");
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new PolicyLoadException("Failed to read policy document: " + path, e);
        }
    }
}
