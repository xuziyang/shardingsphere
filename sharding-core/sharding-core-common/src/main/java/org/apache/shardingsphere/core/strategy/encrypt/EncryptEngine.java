/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.strategy.encrypt;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.api.config.encrypt.EncryptRuleConfiguration;
import org.apache.shardingsphere.api.config.encrypt.EncryptTableRuleConfiguration;
import org.apache.shardingsphere.api.config.encrypt.EncryptorRuleConfiguration;
import org.apache.shardingsphere.core.spi.algorithm.encrypt.ShardingEncryptorServiceLoader;
import org.apache.shardingsphere.spi.encrypt.ShardingEncryptor;
import org.apache.shardingsphere.spi.encrypt.ShardingQueryAssistedEncryptor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Encryptor engine.
 *
 * @author panjuan
 */
@NoArgsConstructor
public final class EncryptEngine {
    
    private final Map<String, ShardingEncryptor> encryptors = new LinkedHashMap<>();
    
    private final Map<String, EncryptTable> tables = new LinkedHashMap<>();
    
    public EncryptEngine(final EncryptRuleConfiguration encryptRuleConfiguration) {
        initEncryptors(encryptRuleConfiguration.getEncryptors());
        initTables(encryptRuleConfiguration.getTables());
    }
    
    private void initEncryptors(final Map<String, EncryptorRuleConfiguration> encryptors) {
        ShardingEncryptorServiceLoader serviceLoader = new ShardingEncryptorServiceLoader();
        for (Entry<String, EncryptorRuleConfiguration> each : encryptors.entrySet()) {
            this.encryptors.put(each.getKey(), createShardingEncryptor(serviceLoader, each.getValue()));
        }
    }
    
    private ShardingEncryptor createShardingEncryptor(final ShardingEncryptorServiceLoader serviceLoader, final EncryptorRuleConfiguration encryptorRuleConfiguration) {
        ShardingEncryptor encryptor = serviceLoader.newService(encryptorRuleConfiguration.getType(), encryptorRuleConfiguration.getProperties());
        encryptor.init();
        return encryptor;
    }
    
    private void initTables(final Map<String, EncryptTableRuleConfiguration> tables) {
        for (Entry<String, EncryptTableRuleConfiguration> entry : tables.entrySet()) {
            this.tables.put(entry.getKey(), new EncryptTable(entry.getValue()));
        }
    }
    
    /**
     * Get sharding encryptor.
     * 
     * @param logicTable logic table name
     * @param logicColumn column name
     * @return optional of sharding encryptor
     */
    public Optional<ShardingEncryptor> getShardingEncryptor(final String logicTable, final String logicColumn) {
        if (!tables.containsKey(logicTable)) {
            return Optional.absent();
        }
        Optional<String> encryptor = tables.get(logicTable).getShardingEncryptor(logicColumn);
        return encryptor.isPresent() ? Optional.of(encryptors.get(encryptor.get())) : Optional.<ShardingEncryptor>absent();
    }
    
    /**
     * Get encrypt table names.
     *
     * @return encrypt table names
     */
    public Collection<String> getEncryptTableNames() {
        return tables.keySet();
    }
    
    /**
     * Get logic column.
     * 
     * @param logicTable logic table
     * @param cipherColumn cipher column
     * @return logic column
     */
    public String getLogicColumn(final String logicTable, final String cipherColumn) {
        return tables.get(logicTable).getLogicColumn(cipherColumn);
    }
    
    /**
     * Get plain column.
     *
     * @param logicTable logic table name
     * @param logicColumn logic column name
     * @return plain column
     */
    public Optional<String> getPlainColumn(final String logicTable, final String logicColumn) {
        return tables.get(logicTable).getPlainColumn(logicColumn);
    }
    
    /**
     * Get plain columns.
     *
     * @param logicTable logic table name
     * @return plain columns
     */
    public Collection<String> getPlainColumns(final String logicTable) {
        if (!tables.containsKey(logicTable)) {
            return Collections.emptyList();
        }
        return tables.get(logicTable).getPlainColumns();
    }
    
    /**
     * Is has plain column or not.
     *
     * @param logicTable logic table name
     * @return has plain column or not
     */
    public boolean isHasPlainColumn(final String logicTable) {
        return tables.containsKey(logicTable) && tables.get(logicTable).isHasPlainColumn();
    }
    
    /**
     * Get cipher column.
     * 
     * @param logicTable logic table name
     * @param logicColumn logic column name
     * @return cipher column
     */
    public String getCipherColumn(final String logicTable, final String logicColumn) {
        return tables.get(logicTable).getCipherColumn(logicColumn);
    }
    
    /**
     * Get cipher columns.
     *
     * @param logicTable logic table name
     * @return cipher columns
     */
    public Collection<String> getCipherColumns(final String logicTable) {
        if (!tables.containsKey(logicTable)) {
            return Collections.emptyList();
        }
        return tables.get(logicTable).getCipherColumns();
    }
    
    /**
     * Get assisted query column.
     * 
     * @param logicTable logic table name
     * @param logicColumn column name
     * @return assisted query column
     */
    public Optional<String> getAssistedQueryColumn(final String logicTable, final String logicColumn) {
        if (!tables.containsKey(logicTable)) {
            return Optional.absent();
        }
        return tables.get(logicTable).getAssistedQueryColumn(logicColumn);
    }
    
    /**
     * Get assisted query columns.
     *
     * @param logicTable logic table name
     * @return assisted query columns
     */
    public Collection<String> getAssistedQueryColumns(final String logicTable) {
        if (!tables.containsKey(logicTable)) {
            return Collections.emptyList();
        }
        return tables.get(logicTable).getAssistedQueryColumns();
    }
    
    /**
     * Is has query assisted column or not.
     *
     * @param logicTable logic table name
     * @return has query assisted column or not
     */
    public boolean isHasQueryAssistedColumn(final String logicTable) {
        return tables.containsKey(logicTable) && tables.get(logicTable).isHasQueryAssistedColumn();
    }
    
    /**
     * Get assisted query and plain column count.
     * 
     * @param logicTable logic table name
     * @return assisted query and plain column count
     */
    public Integer getAssistedQueryAndPlainColumnCount(final String logicTable) {
        return getAssistedQueryColumns(logicTable).size() + getPlainColumns(logicTable).size();
    }
    
    /**
     * Get encrypt assisted column values.
     * 
     * @param logicTable logic table
     * @param logicColumn logic column
     * @param originalColumnValues original column values
     * @return assisted column values
     */
    public List<Object> getEncryptAssistedColumnValues(final String logicTable, final String logicColumn, final List<Object> originalColumnValues) {
        final Optional<ShardingEncryptor> shardingEncryptor = getShardingEncryptor(logicTable, logicColumn);
        Preconditions.checkArgument(shardingEncryptor.isPresent() && shardingEncryptor.get() instanceof ShardingQueryAssistedEncryptor,
                String.format("Can not find ShardingQueryAssistedEncryptor by %s.%s.", logicTable, logicColumn));
        return Lists.transform(originalColumnValues, new Function<Object, Object>() {
            
            @Override
            public Object apply(final Object input) {
                return ((ShardingQueryAssistedEncryptor) shardingEncryptor.get()).queryAssistedEncrypt(input.toString());
            }
        });
    }
    
    /**
     * get encrypt column values.
     *
     * @param logicTable logic table
     * @param logicColumn logic column
     * @param originalColumnValues original column values
     * @return encrypt column values
     */
    public List<Object> getEncryptColumnValues(final String logicTable, final String logicColumn, final List<Object> originalColumnValues) {
        final Optional<ShardingEncryptor> shardingEncryptor = getShardingEncryptor(logicTable, logicColumn);
        Preconditions.checkArgument(shardingEncryptor.isPresent(), String.format("Can not find ShardingQueryAssistedEncryptor by %s.%s.", logicTable, logicColumn));
        return Lists.transform(originalColumnValues, new Function<Object, Object>() {
            
            @Override
            public Object apply(final Object input) {
                return String.valueOf(shardingEncryptor.get().encrypt(input.toString()));
            }
        });
    }
}
