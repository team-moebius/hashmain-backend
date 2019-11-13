import * as React from 'react';
import { withAlert, AlertManager } from 'react-alert';

import ApiKeyRegistBox from 'components/organisms/ApiKeyRegistBox';
import Paper from 'components/atoms/Paper';
import PageTemplate from 'components/templates/PageTemplate';
import Test1 from 'pages/sub-contents/Test1';
import Test2 from 'pages/sub-contents/Test2';
import ajax from 'utils/Ajax';

import 'assets/scss/HtsConfig.scss';

interface HtsConfigProps {
  alert: AlertManager;
}

interface HtsConfigState {
  index: number;
}

class HtsConfig extends React.Component<HtsConfigProps, HtsConfigState> {
  private static TAB_LABELS = ['Test1', 'Test2'];
  private static TAB_ITEMS = [<Test1 />, <Test2 />];
  constructor(props: HtsConfigProps) {
    super(props);
    this.state = {
      index: 0,
    };
  }

  onChangeTabIndex = (e: React.ChangeEvent<{}>, value: any) => {
    this.setState({ index: value });
  };

  onClickApiRegistButton = (data: object) => {
    console.log(data);
    ajax
      .post('/api/api-keys', data)
      .then(response => {
        this.props.alert.success('등록 성공');
      })
      .catch(error => {
        this.props.alert.error('등록 실패');
      });
  };

  onClickViewMyApkKeyButton = () => {
    ajax
      .get('/api')
      .then(reponse => {
        // this.props.alert.success('등록 성공');
      })
      .catch(error => {
        // this.props.alert.error('등록 실패');
      });
  };

  render() {
    return (
      <div className="hts-config">
        <Paper className="hts-config__tab">
          <PageTemplate
            index={this.state.index}
            onChangeTab={this.onChangeTabIndex}
            tabLabels={HtsConfig.TAB_LABELS}
            tabContents={HtsConfig.TAB_ITEMS}
          />
        </Paper>
        <Paper className="hts-config__sub">
          <ApiKeyRegistBox
            onClickRegistApiKeyButton={this.onClickApiRegistButton}
            onClickViewMyApiKeyButton={this.onClickViewMyApkKeyButton}
          />
        </Paper>
      </div>
    );
  }
}

// @ts-ignore
export default withAlert()(HtsConfig);