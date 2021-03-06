package com.varone.web.aggregator;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

import com.varone.web.eventlog.bean.SparkEventLogBean;
import com.varone.web.eventlog.bean.SparkEventLogBean.TaskEnd;
import com.varone.web.reader.eventlog.impl.EventLogHdfsReaderImpl;
import com.varone.web.util.VarOneConfiguration;
import com.varone.web.vo.HistoryDetailStageVO;
import com.varone.web.vo.TasksVO;

public class UIDataAggregatorTest {

	@Test
	public void testgetHistoryDetialStage() throws Exception{
		VarOneConfiguration varOneConf = new VarOneConfiguration();
		Configuration config = varOneConf.loadHadoopConfiguration();
		
		
		UIDataAggregator aggregator = new UIDataAggregator();
		EventLogHdfsReaderImpl hdfsReader = new EventLogHdfsReaderImpl(config);
		SparkEventLogBean sparkEventLog = hdfsReader.getHistoryStageDetails("application_1449569227858_0664");
		
		HistoryDetailStageVO historyDetailsVO = aggregator.aggregateHistoryDetialStage(sparkEventLog, 1);
		List<TasksVO> taskVOList = historyDetailsVO.getTasks();
		
		/*for(TasksVO taskVO : taskVOList){
			System.out.println(taskVO.getIndex() + "   " + taskVO.getGcTime());
		}*/
		
	}
	

	@Test
	public void testMedian(){
		int[] a = {1, 2, 3, 4, 5, 6};
		double result = 0;
		int n = a.length;

		if((a.length % 2) == 0){
			result = (a[n / 2] + a[(n / 2) - 1]) / 2.0;
		}else{
			result = a[(n - 1) / 2];
		}
		System.out.println(result);
	}
}
