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

public class Constants {
    /**
     * Migration client argument property names.
     */
    public static final String ARG_MIGRATE_TENANTS = "tenants";
    public static final String ARG_MIGRATE_TENANTS_RANGE = "tenantRange";
    public static final String ARG_MIGRATE_BLACKLIST_TENANTS = "blackListed";
    public static final String ARG_MIGRATE_ACCESS_CONTROL = "migrateAccessControl";

    /**
     * Publisher Access Control related registry properties and values.
     */
    public static final String PUBLISHER_ROLES = "publisher_roles";
    public static final String ACCESS_CONTROL = "publisher_access_control";
    public static final String NO_ACCESS_CONTROL = "all";
    public static final String NULL_USER_ROLE_LIST = "null";
    public static final String STORE_VIEW_ROLES = "store_view_roles";
    public static final String PUBLIC_STORE_VISIBILITY = "public";
    public static final String RESTRICTED_STORE_VISIBILITY = "restricted";
    public static final String PRIVATE_STORE_VISIBILITY = "private";
    public static final String API_OVERVIEW_VISIBILITY = "overview_visibility";
    public static final String API_OVERVIEW_VISIBLE_ROLES = "overview_visibleRoles";
}
