package SA.irms.common.identity;

public interface SharedIdentityPolicyPort {
    PolicySnapshot getPolicySnapshot();

    BranchView findDefaultBranch();
}
