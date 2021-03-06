/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.expression.function.aggregate;

import org.elasticsearch.xpack.sql.expression.Expression;
import org.elasticsearch.xpack.sql.expression.Expressions.ParamOrdinal;
import org.elasticsearch.xpack.sql.expression.Foldables;
import org.elasticsearch.xpack.sql.tree.Location;
import org.elasticsearch.xpack.sql.tree.NodeInfo;
import org.elasticsearch.xpack.sql.type.DataType;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.elasticsearch.xpack.sql.expression.TypeResolutions.isFoldable;
import static org.elasticsearch.xpack.sql.expression.TypeResolutions.isNumeric;

public class Percentile extends NumericAggregate implements EnclosedAgg {

    private final Expression percent;

    public Percentile(Location location, Expression field, Expression percent) {
        super(location, field, singletonList(percent));
        this.percent = percent;
    }

    @Override
    protected NodeInfo<Percentile> info() {
        return NodeInfo.create(this, Percentile::new, field(), percent);
    }

    @Override
    public Percentile replaceChildren(List<Expression> newChildren) {
        if (newChildren.size() != 2) {
            throw new IllegalArgumentException("expected [2] children but received [" + newChildren.size() + "]");
        }
        return new Percentile(location(), newChildren.get(0), newChildren.get(1));
    }

    @Override
    protected TypeResolution resolveType() {
        TypeResolution resolution = isFoldable(percent, functionName(), ParamOrdinal.SECOND);
        if (resolution.unresolved()) {
            return resolution;
        }

        resolution = super.resolveType();
        if (resolution.unresolved()) {
            return resolution;
        }

        return isNumeric(percent, functionName(), ParamOrdinal.DEFAULT);
    }

    public Expression percent() {
        return percent;
    }

    @Override
    public DataType dataType() {
        return DataType.DOUBLE;
    }

    @Override
    public String innerName() {
        return Double.toString(Foldables.doubleValueOf(percent));
    }
}
