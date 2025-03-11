import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      links={[
        {
          key: '律π--知识管理',
          title: '律π--知识管理',
          href: '',
          blankTarget: true,
        },

        {
          key: 'github',
          title: <GithubOutlined />,
          href: 'https://github.com/ant-design/ant-design-pro',
          blankTarget: true,
        },
        {
          key: 'Design for 零感',
          title: 'Design for 零感',
          href: '',
          blankTarget: true,
        },

      ]}
    />
  );
};

export default Footer;
