/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.registry.migration.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.migration.client.internal.ServiceHolder;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;

public class RegistryServiceImpl implements RegistryService {
    private static final Log log = LogFactory.getLog(RegistryServiceImpl.class);
    private Tenant tenant = null;
    private APIProvider apiProvider = null;

    @Override
    public void startTenantFlow(Tenant tenant) {
        if (this.tenant != null) {
            log.error("Start tenant flow called without ending previous tenant flow");
            throw new IllegalStateException("Previous tenant flow has not been ended, " +
                    "'RegistryService.endTenantFlow()' needs to be called");
        } else {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);
            this.tenant = tenant;
        }
    }

    @Override
    public void endTenantFlow() {
        if (this.tenant == null) {
            log.error("End tenant flow called even though tenant flow has already been ended or was not started");
            throw new IllegalStateException("Previous tenant flow has already been ended, " +
                    "unnecessary additional RegistryService.endTenantFlow()' call has been detected");
        } else {
            PrivilegedCarbonContext.endTenantFlow();
            this.tenant = null;
            this.apiProvider = null;
        }
    }

    @Override
    public GenericArtifact[] getGenericAPIArtifacts() {
        log.debug("Calling getGenericAPIArtifacts");
        GenericArtifact[] artifacts = {};

        try {
            Registry registry = getGovernanceRegistry();
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

            if (artifactManager != null) {
                artifacts = artifactManager.getAllGenericArtifacts();

                log.debug("Total number of api artifacts : " + artifacts.length);
            } else {
                log.debug("No api artifacts found in registry for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }

        } catch (RegistryException e) {
            log.error("Error occurred when getting GenericArtifacts from registry", e);
        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (APIManagementException e) {
            log.error("Failed to initialize GenericArtifactManager", e);
        }
        return artifacts;
    }

    @Override
    public void updateGenericAPIArtifactsForAccessControl(String resourcePath, GenericArtifact artifact) {
        try {
            Registry registry = getGovernanceRegistry();
            Resource resource = registry.get(resourcePath);
            boolean isResourceUpdated = false;

            if (resource != null) {
                String publisherAccessControl = resource.getProperty(Constants.PUBLISHER_ROLES);

                if (publisherAccessControl == null || publisherAccessControl.trim().isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("API at " + resourcePath + "did not have property : " + Constants.PUBLISHER_ROLES
                                + ", hence adding the null value for that API resource.");
                    }
                    resource.setProperty(Constants.PUBLISHER_ROLES, Constants.NULL_USER_ROLE_LIST);
                    resource.setProperty(Constants.ACCESS_CONTROL, Constants.NO_ACCESS_CONTROL);
                    isResourceUpdated = true;
                }

                String storeViewRoles = resource.getProperty(Constants.STORE_VIEW_ROLES);
                String storeVisibility = artifact.getAttribute(Constants.API_OVERVIEW_VISIBILITY);
                String storeVisibleRoles = artifact.getAttribute(Constants.API_OVERVIEW_VISIBLE_ROLES);

                if (storeViewRoles == null) {
                    if (Constants.PUBLIC_STORE_VISIBILITY.equals(storeVisibility) || publisherAccessControl == null ||
                            publisherAccessControl.trim().isEmpty() || publisherAccessControl.equals(Constants.NULL_USER_ROLE_LIST)) {
                        if (log.isDebugEnabled()) {
                            log.debug("API at " + resourcePath + "has the public visibility, but  : "
                                    + Constants.STORE_VIEW_ROLES + " property is not set to "
                                    + Constants.NULL_USER_ROLE_LIST + ". Hence setting the correct value.");
                        }
                        resource.setProperty(Constants.STORE_VIEW_ROLES, Constants.NULL_USER_ROLE_LIST);
                        isResourceUpdated = true;
                    } else {
                        StringBuilder combinedRoles = new StringBuilder(publisherAccessControl);
                        String[] roles = storeVisibleRoles.split(",");

                        for (String role : roles) {
                            combinedRoles.append(",").append(role.trim().toLowerCase());
                        }
                        resource.setProperty(Constants.STORE_VIEW_ROLES, String.valueOf(combinedRoles));
                        isResourceUpdated = true;
                    }
                }
                if (isResourceUpdated) {
                    registry.put(resourcePath, resource);
                }
            }
        } catch (RegistryException e) {
            log.error("Error occurred when updating GenericArtifacts in registry for the Publisher Access Control feature.", e);
        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        }
    }

    @Override
    public boolean isGovernanceRegistryResourceExists(String registryLocation) throws UserStoreException, RegistryException {
        return getGovernanceRegistry().resourceExists(registryLocation);
    }

    @Override
    public Object getGovernanceRegistryResource(final String registryLocation) throws UserStoreException, RegistryException {
        Object content = null;
        Registry registry = getGovernanceRegistry();
        if (registry.resourceExists(registryLocation)) {
            Resource resource = registry.get(registryLocation);
            content = resource.getContent();
        }
        return content;
    }

    public Registry getGovernanceRegistry() throws UserStoreException, RegistryException {
        if (tenant == null) {
            throw new IllegalStateException("The tenant flow has not been started, " +
                    "'RegistryService.startTenantFlow(Tenant tenant)' needs to be called");
        }

        String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).
                getRealmConfiguration().getAdminUserName();
        log.debug("Tenant admin username : " + adminName);
        ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
        return ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant.getId());
    }
}
