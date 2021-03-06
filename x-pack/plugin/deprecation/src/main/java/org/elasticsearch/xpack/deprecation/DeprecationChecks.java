/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.deprecation;

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.xpack.core.deprecation.DeprecationInfoAction;
import org.elasticsearch.xpack.core.deprecation.DeprecationIssue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class containing all the cluster, node, and index deprecation checks that will be served
 * by the {@link DeprecationInfoAction}.
 */
public class DeprecationChecks {

    private DeprecationChecks() {
    }

    static List<Function<ClusterState, DeprecationIssue>> CLUSTER_SETTINGS_CHECKS =
        Collections.unmodifiableList(Arrays.asList(
            ClusterDeprecationChecks::checkShardLimit,
            ClusterDeprecationChecks::checkClusterName
        ));

    static List<BiFunction<List<NodeInfo>, List<NodeStats>, DeprecationIssue>> NODE_SETTINGS_CHECKS =
        Collections.unmodifiableList(Arrays.asList(
            NodeDeprecationChecks::httpEnabledSettingRemoved,
            NodeDeprecationChecks::auditLogPrefixSettingsCheck,
            NodeDeprecationChecks::indexThreadPoolCheck,
            NodeDeprecationChecks::bulkThreadPoolCheck,
            NodeDeprecationChecks::tribeNodeCheck,
            NodeDeprecationChecks::httpPipeliningCheck,
            NodeDeprecationChecks::discoveryConfigurationCheck,
            NodeDeprecationChecks::azureRepositoryChanges,
            NodeDeprecationChecks::gcsRepositoryChanges,
            NodeDeprecationChecks::fileDiscoveryPluginRemoved
        ));

    static List<Function<IndexMetaData, DeprecationIssue>> INDEX_SETTINGS_CHECKS =
        Collections.unmodifiableList(Arrays.asList(
            IndexDeprecationChecks::oldIndicesCheck,
            IndexDeprecationChecks::delimitedPayloadFilterCheck,
            IndexDeprecationChecks::percolatorUnmappedFieldsAsStringCheck,
            IndexDeprecationChecks::indexNameCheck,
			IndexDeprecationChecks::nodeLeftDelayedTimeCheck,
            IndexDeprecationChecks::shardOnStartupCheck,
            IndexDeprecationChecks::classicSimilarityMappingCheck,
            IndexDeprecationChecks::classicSimilaritySettingsCheck
            ));

    /**
     * helper utility function to reduce repeat of running a specific {@link Set} of checks.
     *
     * @param checks The functional checks to execute using the mapper function
     * @param mapper The function that executes the lambda check with the appropriate arguments
     * @param <T> The signature of the check (BiFunction, Function, including the appropriate arguments)
     * @return The list of {@link DeprecationIssue} that were found in the cluster
     */
    static <T> List<DeprecationIssue> filterChecks(List<T> checks, Function<T, DeprecationIssue> mapper) {
        return checks.stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
