package com.simon.king.server.task;

import com.alibaba.fastjson.JSON;
import com.simon.king.core.meta.TaskEnum;
import com.simon.king.groovy.ParserService;
import com.simon.king.server.KingServerConstant;
import com.simon.king.server.service.TaskExtService;
import com.simon.king.server.task.monitor.TaskMonitorEntity;
import com.simon.king.server.util.BeanUtils;
import com.simon.neo.NeoMap;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * job的回调执行器
 *
 * @author zhouzhenyong
 * @since 2019/1/16 下午7:49
 */
public class TaskJob implements Job {

    private TaskExtService taskExtService = BeanUtils.getBean(TaskExtService.class);
    private ParserService parserService = BeanUtils.getBean(ParserService.class);
    private StringRedisTemplate stringRedisTemplate = BeanUtils.getBean(StringRedisTemplate.class);

    @Override
    public void execute(JobExecutionContext context) {

        if (null != taskExtService && null != parserService) {

            String id = String.valueOf(context.getMergedJobDataMap().get("id"));
            NeoMap taskRecord = taskExtService.one(Long.valueOf(id));

            String taskType = taskRecord.getStr("task_type");
            if (taskType.equals(TaskEnum.GROOVY.getName())) {
                // 后面这个参数暂时没有想到有哪些外来参数，因此暂时这是为null
                TaskMonitorEntity monitorEntity = TaskMonitorEntity.build(taskRecord);
                addTaskFlag(monitorEntity);
                parserService.parse(taskRecord.getStr("data"), null);
                removeTaskFlag(monitorEntity);
            } else {
                // 另外一个类型以后遗留
            }
        }
    }

    /**
     * 添加任务执行的Flag
     */
    private void addTaskFlag(TaskMonitorEntity monitorEntity) {
        stringRedisTemplate.opsForSet().add(KingServerConstant.TASK_MONITOR_KEY, JSON.toJSONString(monitorEntity));
    }

    /**
     * 删除任务执行的Flag
     */
    private void removeTaskFlag(TaskMonitorEntity monitorEntity) {
        stringRedisTemplate.opsForSet().remove(KingServerConstant.TASK_MONITOR_KEY, JSON.toJSONString(monitorEntity));
    }
}

