/**
 * 
 */
package com.varone.web.facade;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.var.web.util.VarOneEnv;
import com.varone.node.MetricsType;
import com.varone.web.aggregator.UIDataAggregator;
import com.varone.web.eventlog.bean.SparkEventLogBean;
import com.varone.web.eventlog.bean.SparkEventLogBean.AppStart;
import com.varone.web.metrics.bean.NodeBean;
import com.varone.web.reader.eventlog.EventLogReader;
import com.varone.web.reader.eventlog.impl.EventLogHdfsReaderImpl;
import com.varone.web.reader.metrics.MetricsReader;
import com.varone.web.reader.metrics.impl.MetricsRpcReaderImpl;
import com.varone.web.vo.DefaultApplicationVO;
import com.varone.web.vo.DefaultNodeVO;
import com.varone.web.vo.DefaultTotalNodeVO;
import com.varone.web.vo.HistoryVO;
import com.varone.web.vo.JobVO;
import com.varone.web.vo.StageVO;
import com.varone.web.yarn.service.YarnService;

/**
 * @author allen
 *
 */
public class SparkMonitorFacade {
	
	private Configuration config;
	
	public SparkMonitorFacade() {
		VarOneEnv env = new VarOneEnv();
		this.config = this.loadConfiguration(env.getVarOneConfPath());		
	}
	
	public DefaultTotalNodeVO getDefaultClusterDashBoard(List<String> metrics) throws Exception{
		DefaultTotalNodeVO result = null;
		YarnService yarnService = new YarnService(this.config);
				
		Map<String, List<NodeBean>> nodeMetricsByAppId = new LinkedHashMap<String, List<NodeBean>>();
		
		try {
			List<String> allNodeHost = yarnService.getAllNodeHost();
			List<String> runningSparkAppId = yarnService.getRunningSparkApplications();
			
			EventLogReader eventLogReader = new EventLogHdfsReaderImpl(this.config);
			MetricsReader metricsReader = new MetricsRpcReaderImpl(allNodeHost); 
			
			for(String applicationId: runningSparkAppId){
				nodeMetricsByAppId.put(applicationId, 
						metricsReader.getAllNodeMetrics(applicationId, metrics));
			}
			
			Map<String, SparkEventLogBean> inProgressEventLogByAppId = eventLogReader.getAllInProgressLog();
			
			result = new UIDataAggregator().aggregateClusterDashBoard(metrics, runningSparkAppId, allNodeHost, 
							nodeMetricsByAppId, inProgressEventLogByAppId);
			
		} finally{
			yarnService.close();
		}
		
		return result;
	}
	
	
	public DefaultApplicationVO getJobDashBoard(String applicationId, List<String> metrics) throws Exception{
		DefaultApplicationVO result = null;
		YarnService yarnService = new YarnService(this.config);
		
		if(!metrics.contains(MetricsType.EXEC_THREADPOOL_COMPLETETASK))
			metrics.add(MetricsType.EXEC_THREADPOOL_COMPLETETASK.name());
		
		try{
			List<String> allNodeHost = yarnService.getAllNodeHost();
			
			if(yarnService.isStartRunningSparkApplication(applicationId)){
				EventLogReader eventLogReader = new EventLogHdfsReaderImpl(this.config);
				MetricsReader metricsReader = new MetricsRpcReaderImpl(allNodeHost); 
				
				SparkEventLogBean inProgressLog = eventLogReader.getInProgressLog(applicationId);
				List<NodeBean> nodeMetrics = metricsReader.getAllNodeMetrics(applicationId, metrics);
				
				result = new UIDataAggregator().aggregateJobDashBoard(metrics, allNodeHost, inProgressLog, nodeMetrics);
			}
		} finally{
			yarnService.close();
		}
		return result;
	}
	
	public DefaultNodeVO getNodeDashBoard(String node, List<String> metrics) throws Exception {
		DefaultNodeVO result = null;
		YarnService yarnService = new YarnService(this.config);
		Map<String, NodeBean> nodeMetricsByAppId = new LinkedHashMap<String, NodeBean>();
		
		MetricsReader metricsReader = new MetricsRpcReaderImpl(); 
		try {
			List<String> runningSparkAppId = yarnService.getRunningSparkApplications();
			for(String applicationId: runningSparkAppId){
				nodeMetricsByAppId.put(applicationId, 
						metricsReader.getNodeMetrics(node, applicationId, metrics));
			}
						
			result = new UIDataAggregator().aggregateNodeDashBoard(metrics, runningSparkAppId, node, nodeMetricsByAppId);
			
		} finally{
			yarnService.close();
		}
		return result;
	}
	
	public List<String> getRunningJobs() throws Exception {
		YarnService yarnService = new YarnService(this.config);
		return yarnService.getRunningSparkApplications();
	}

	public List<String> getNodeLists() throws Exception{
		List<String> nodes = new ArrayList<String>();
		YarnService yarnService = new YarnService(this.config);
		try{
			nodes = yarnService.getAllNodeHost();
		} finally {
			yarnService.close();
		}
		
		return nodes;
	}

	public List<HistoryVO> getAllSparkApplication() throws Exception {
		List<HistoryVO> histories = new ArrayList<HistoryVO>();
		EventLogReader eventLogReader = new EventLogHdfsReaderImpl(this.config);
		List<SparkEventLogBean> allSparkAppLog = eventLogReader.getAllSparkAppLog();
		
		for(SparkEventLogBean eventLog: allSparkAppLog){
			AppStart appStart = eventLog.getAppStart();
			HistoryVO history = new HistoryVO();
			history.setId(appStart.getId());
			history.setName(appStart.getName());
			history.setStartTime(appStart.getTimestamp()+"");
			if(eventLog.getAppEnd() != null){
				history.setEndTime(eventLog.getAppEnd().getTimestamp()+"");
			}
			history.setUser(appStart.getUser());
			histories.add(history);
		}
		
		return histories;
	}

	public List<JobVO> getSparkApplicationJobs(String applicationId) throws Exception {
		EventLogReader eventLogReader = new EventLogHdfsReaderImpl(this.config);
		SparkEventLogBean eventLog = eventLogReader.getApplicationJobs(applicationId);
		
		return new UIDataAggregator().aggregateApplicationJobs(applicationId, eventLog);
	}

	public List<StageVO> getSparkJobStages(String applicationId, String jobId) throws Exception {
		EventLogReader eventLogReader = new EventLogHdfsReaderImpl(this.config);
		SparkEventLogBean eventLog = eventLogReader.getJobStages(applicationId, jobId);
		
		return new UIDataAggregator().aggregateJobStages(applicationId, jobId, eventLog);
	}
	
	protected Configuration loadConfiguration(File varOneConfPath) {
		Configuration config = new Configuration();
		for(File file : varOneConfPath.listFiles()){
			if(file.getName().endsWith(".xml")){
				config.addResource(new Path(file.getAbsolutePath()));
			}
		}
		this.checkConfig(config, varOneConfPath, "fs.default.name");
		this.checkConfig(config, varOneConfPath, "yarn.resourcemanager.address");
		this.checkConfig(config, varOneConfPath, "spark.eventLog.dir");
		
		return config;
	}
	
	private void checkConfig(Configuration config, File varOneConfPath, String key){
		String value = config.get(key);
		if(value == null){
			throw new RuntimeException(varOneConfPath.getAbsolutePath() + "/*.xml not set " + key + " property");
		}
	}
}
