import React, { useState } from 'react'
import { Row, Col, Menu, Icon } from 'antd'
import Login from './login'
import Join from './join'
import '../../style/sign.css'


function Sign() {
  const [viewMode, setViewMode] = useState('login')

  return (
    <>
      <Row type='flex' justify='space-around' align='middle' style={{ height: '100vh' }}>
        <Col span={10}>
          <div className='signPageBox'>
            <Menu
              onClick={(e) => setViewMode(e.key)}
              selectedKeys={[viewMode]}
              mode='horizontal'
              style={{ textAlign: 'center', width: '100%' }}
            >
              <Menu.Item key='login' style={{ width: '180px' }}>
                <Icon type='key' />로그인
              </Menu.Item>
              <Menu.Item key='join' style={{ width: '180px' }}>
                <Icon type='user-add' />회원가입
              </Menu.Item>
            </Menu>
            {viewMode === 'login' ? <Login /> : <Join />}
          </div>
        </Col>
      </Row>
    </>
  )
}

export default Sign
