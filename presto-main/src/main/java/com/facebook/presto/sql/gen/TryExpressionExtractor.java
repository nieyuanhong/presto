/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.sql.gen;

import com.facebook.presto.sql.relational.CallExpression;
import com.facebook.presto.sql.relational.ConstantExpression;
import com.facebook.presto.sql.relational.InputReferenceExpression;
import com.facebook.presto.sql.relational.RowExpression;
import com.facebook.presto.sql.relational.RowExpressionVisitor;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.facebook.presto.sql.relational.Signatures.TRY;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

public class TryExpressionExtractor
{
    private TryExpressionExtractor()
    {
    }

    public static List<RowExpression> extractTryExpressions(RowExpression expression)
    {
        Visitor visitor = new Visitor();
        expression.accept(visitor, new Context());
        return visitor.getTryExpressionsPostOrder();
    }

    private static class Visitor
            implements RowExpressionVisitor<Context, Void>
    {
        private final ImmutableList.Builder<RowExpression> tryExpressions = ImmutableList.builder();

        @Override
        public Void visitInputReference(InputReferenceExpression node, Context context)
        {
            // TODO: change such that CallExpressions only capture the inputs they actually depend on
            return null;
        }

        @Override
        public Void visitCall(CallExpression call, Context context)
        {
            boolean isTry = call.getSignature().getName().equals(TRY);
            if (isTry) {
                checkState(call.getArguments().size() == 1, "try call expressions must have a single argument");
                checkState(getOnlyElement(call.getArguments()) instanceof CallExpression, "try call expression argument must be a call expression");
            }

            for (RowExpression rowExpression : call.getArguments()) {
                rowExpression.accept(this, null);
            }

            if (isTry) {
                tryExpressions.add(getOnlyElement(call.getArguments()));
            }

            return null;
        }

        @Override
        public Void visitConstant(ConstantExpression literal, Context context)
        {
            return null;
        }

        public List<RowExpression> getTryExpressionsPostOrder()
        {
            return tryExpressions.build();
        }
    }

    private static class Context
    {
    }
}
