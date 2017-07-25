package org.jenkinsci.plugins.blueoceandisplayurl;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Util;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.blueocean.rest.factory.organization.OrganizationFactory;
import io.jenkins.blueocean.rest.model.BlueOrganization;
import jenkins.branch.MultiBranchProject;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Iterator;
import java.util.Set;


/**
 *`@author Ivan Meredith
 */
@Extension
public class BlueOceanDisplayURLImpl extends DisplayURLProvider {

    private static final Set<String> SUPPORTED_RUNS = ImmutableSet.of(
            FreeStyleBuild.class.getName(),
            WorkflowRun.class.getName(),
            "hudson.maven.AbstractMavenBuild"
    );

    private static final Set<String> SUPPORTED_JOBS = ImmutableSet.of(
            WorkflowJob.class.getName(),
            MultiBranchProject.class.getName(),
            FreeStyleProject.class.getName(),
            "hudson.maven.AbstractMavenProject"
    );

    @Override
    public String getDisplayName() {
        return "Blue Ocean";
    }

    @Override
    public String getRoot() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not started");
        }
        String root = jenkins.getRootUrl();
        if (root == null) {
            root = "http://unconfigured-jenkins-location/";
        }
        return root + "blue/";
    }

    @Override
    public String getRunURL(Run<?, ?> run) {
        if (isSupported(run)) {
            if (run instanceof WorkflowRun) {
                WorkflowJob job = ((WorkflowRun) run).getParent();
                if (job.getParent() instanceof MultiBranchProject) {
                    return getJobURL(((MultiBranchProject) job.getParent())) + "detail/" + Util.rawEncode(job.getDisplayName()) + "/" + run.getNumber() + "/";
                }
            }
            Job job = run.getParent();
            return getJobURL(job) + "detail/" + Util.rawEncode(job.getDisplayName()) + "/" + run.getNumber() + "/";
        } else {
            return DisplayURLProvider.getDefault().getRunURL(run);
        }
    }

    @Override
    public String getChangesURL(Run<?, ?> run) {
        if (isSupported(run)) {
            return getRunURL(run) + "changes";
        } else {
            return DisplayURLProvider.getDefault().getChangesURL(run);
        }
    }

    @Override
    public String getJobURL(Job<?, ?> job) {
        if (isSupported(job)) {
            String jobPath;
            String organization;
            if(job.getParent() instanceof MultiBranchProject) {
                ItemGroup parent = job.getParent();
                jobPath = Util.rawEncode(parent.getFullName());
                organization = getOrganization(parent);
            } else {
                jobPath = Util.rawEncode(job.getFullName());
                organization = getOrganization(job);
            }
            return getRoot() + "organizations/" + organization + "/" + jobPath + "/";
        } else {
            return DisplayURLProvider.getDefault().getJobURL(job);
        }
    }

    private static boolean isSupported(Run<?, ?> run) {
        return isInstance(run, SUPPORTED_RUNS);
    }

    private static boolean isSupported(Job<?, ?> job) {
        return isInstance(job, SUPPORTED_JOBS);
    }

    private static boolean isInstance(Object o, Set<String> clazzes) {
        for (String clazz : clazzes) {
            if (o != null && o.getClass().getName().equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    private String getJobURL(MultiBranchProject<?, ?> project) {
        String jobPath = Util.rawEncode(project.getFullName());
        return getRoot() + "organizations/" + getOrganization((ItemGroup) project) + "/" + jobPath + "/";
    }

    private final String DEFAULT_ORG = "jenkins";

    private String getOrganization(ItemGroup group) {
        try {
            OrganizationFactory orgFactory = OrganizationFactory.getInstance();
            BlueOrganization org = orgFactory.getContainingOrg(group);
            if (org != null) {
                return org.getName();
            } else {
                return getFirstOrg(orgFactory);
            }
        } catch (Exception e) { //There may be no OrganizationFactory which will return a RuntimeException
            return DEFAULT_ORG;
        }

    }

    private String getOrganization(Item item) {
        try {
            OrganizationFactory orgFactory = OrganizationFactory.getInstance();
            BlueOrganization org = orgFactory.getContainingOrg(item);
            if (org != null) {
                return org.getName();
            } else {
                return getFirstOrg(orgFactory);
            }
        } catch (Exception e) { //There may be no OrganizationFactory which will return a RuntimeException
            return DEFAULT_ORG;
        }
    }

    private String getFirstOrg(OrganizationFactory orgFactory) {
        Iterator<BlueOrganization> orgIterator = orgFactory.list().iterator();
        if (orgIterator.hasNext()) {
            return orgIterator.next().getName();
        } else {
            return DEFAULT_ORG;
        }
    }
}
