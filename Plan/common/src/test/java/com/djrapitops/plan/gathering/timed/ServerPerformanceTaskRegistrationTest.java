/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.djrapitops.plan.gathering.timed;

import com.djrapitops.plan.settings.config.PlanConfig;
import com.djrapitops.plan.settings.config.ConfigNode;
import com.djrapitops.plan.settings.config.paths.DataGatheringSettings;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.playeranalytics.plugin.scheduling.RunnableFactory;
import net.playeranalytics.plugin.scheduling.UnscheduledTask;
import net.playeranalytics.plugin.server.PluginLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerPerformanceTaskRegistrationTest {

    @Test
    void serverPerformanceDefaultsToEnabledWhenTheSettingIsMissing() {
        assertTrue(DataGatheringSettings.SERVER_PERFORMANCE.getValueFrom(new ConfigNode(null, null, null)));
    }

    @Test
    void enabledServerPerformanceRegistersTheTpsCounter() {
        PlanConfig config = mock(PlanConfig.class);
        when(config.isFalse(DataGatheringSettings.SERVER_PERFORMANCE)).thenReturn(false);
        RunnableFactory runnableFactory = mock(RunnableFactory.class);
        UnscheduledTask scheduledTask = mock(UnscheduledTask.class);
        TPSCounter counter = new TPSCounter(config, mock(PluginLogger.class), mock(ErrorLogger.class)) {
            @Override
            public void pulse() {
                // Not run by this registration test.
            }
        };
        when(runnableFactory.create(counter)).thenReturn(scheduledTask);

        counter.register(runnableFactory);

        verify(scheduledTask).runTaskTimer(anyLong(), anyLong());
    }

    @Test
    void disabledServerPerformanceDoesNotRegisterPerformanceTasks() {
        PlanConfig config = mock(PlanConfig.class);
        when(config.isFalse(DataGatheringSettings.SERVER_PERFORMANCE)).thenReturn(true);
        RunnableFactory runnableFactory = mock(RunnableFactory.class);

        TPSCounter counter = new TPSCounter(config, mock(PluginLogger.class), mock(ErrorLogger.class)) {
            @Override
            public void pulse() {
                throw new AssertionError("A disabled task must not pulse");
            }
        };
        SystemUsageBuffer buffer = new SystemUsageBuffer();
        SystemUsageBuffer.RamAndCpuTask ramAndCpuTask = new SystemUsageBuffer.RamAndCpuTask(config, buffer, mock(PluginLogger.class));
        SystemUsageBuffer.DiskTask diskTask = new SystemUsageBuffer.DiskTask(config, buffer, mock(PluginLogger.class), mock(ErrorLogger.class));

        counter.register(runnableFactory);
        ramAndCpuTask.register(runnableFactory);
        diskTask.register(runnableFactory);

        verifyNoInteractions(runnableFactory);
    }
}
