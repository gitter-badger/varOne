import React from 'react';
import ClusterSummary from './cluster-summary';
import ClusterMetrics from './cluster-metrics';
import ClusterHeader from './cluster-header';
import MetricsSettingModal from '../commons/metrics-setting-modal';
import ClusterAction from '../../actions/cluster-action';
import ClusterStore from '../../stores/cluster-store';
import connectToStores from 'alt/utils/connectToStores';

@connectToStores
class ClusterContainer extends React.Component {

  fetchInterval = null

  static propTypes = {
    data: React.PropTypes.object
  }
  static getStores(props) {
    return [ClusterStore];
  }
  static getPropsFromStores(props) {
    return ClusterStore.getState();
  }

  constructor(props){
    super(props);
    this.selectMetrics = [];
  }

  componentWillMount(){
    ClusterAction.fetchTotalNodeDashBoard(this.selectMetrics, this.props.period);
  }

  componentWillUnmount(){
    clearInterval(this.fetchInterval);
  }

  componentDidUpdate(){
    clearInterval(this.fetchInterval);
    this.fetchInterval = setInterval(()=>{
        ClusterAction.fetchTotalNodeDashBoard(this.selectMetrics, this.props.period);
    }, 6000);
  }

  handleModalSubmit(selectMetrics){
    this.selectMetrics = selectMetrics;
    clearInterval(this.fetchInterval);
    ClusterAction.fetchTotalNodeDashBoard(selectMetrics, this.props.period);
  }

  handlePeriodSelect = period => {
    clearInterval(this.fetchInterval);
    ClusterAction.fetchTotalNodeDashBoard(this.selectMetrics, period);
  }

  render(){
    if(null !== this.props.data){
      return(
        <div id="page-wrapper">
          <ClusterHeader
            period={this.props.period}
            onPeriodSelect={this.handlePeriodSelect}/>
          <MetricsSettingModal modalTarget="Cluster" onModalSubmit={this.handleModalSubmit.bind(this)}/>
          <div>
            <ClusterSummary
              onNodeClick={this.props.nodeClickCB}
              summaryInfos={this.props.data.displaySummaryInfo}/>
            <ClusterMetrics
              taskStartedNumByNode={this.props.data.taskStartedNumByNode}
              executorNumByNode={this.props.data.executorNumByNode}
              metrics={this.props.data.metrics} />
          </div>
        </div>
      );
    } else {
      return null;
    }
  }
}

export default ClusterContainer;
