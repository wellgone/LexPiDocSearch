import React, { useState, useRef } from 'react';
import { useInstantSearch, useSearchBox } from 'react-instantsearch';
import {
  ModalForm,
  ProFormDigit,
  ProFormSelect,
  ProFormText, ProFormTextArea,
} from '@ant-design/pro-form';
import { Card, Checkbox, Collapse, ConfigProvider, Form, Input, message, Select } from 'antd';
import { SearchProps } from 'antd/es/input';
import { ProForm, ProFormDependency, ProFormList } from '@ant-design/pro-components';
import { ProFormInstance } from '@ant-design/pro-form/lib';
const { Search } = Input;
const { Option } = Select;

function CustomSearchBox(props) {
  const { query, refine } = useSearchBox(props);
  const { status } = useInstantSearch();
  // const [inputValue, setInputValue] = useState(query);
  //匹配类型，精准1，模糊2
  const [matchType, setMatchType] = useState(1);
  //检索类型，普通1，高级2
  const [searchType, setSearchType] = useState(1);
  //是否仅匹配标题
  const [onlyTitle, setOnlyTitle] = useState(false);

  // const isSearchStalled = status === 'stalled';

  function setQuery(newQuery) {
    // setInputValue(newQuery);
    refine(newQuery);
  }

  //点击搜索图标、清除图标，或按下回车键时的回调
  const onSearch: SearchProps['onSearch'] = (value, _e, info) => {
    //当info属于source: 'clear'时，表示清空搜索框
    if (info?.source === 'clear') {
      setQuery('');
      return;
    }

    //当属于input类型且输入为空时才提示
    if (info?.source === 'input' && value.trim() === '') {
      message.info('请输入检索内容');
      return;
    }

    //searchType 1为普通检索，2为高级检索
    const queryData = {
      searchType: searchType,
      queryData: {
        matchType: matchType,
        onlyTitle: onlyTitle,
        queryText: value
      }
    };

    setQuery(JSON.stringify(queryData)); // 合并为一次调用
  };

  const formRef = useRef<ProFormInstance>();
  const [open, setOpen] = useState(false);

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center',justifyContent:'center' }}>
      <ConfigProvider
        theme={{
          token: {
            controlHeightLG: 48,
          },
        }}
      >
        <Search

          style={{ flexGrow: 1, width: 'auto',marginLeft:7 }}
          addonBefore={
            <Select 
              defaultValue={1} 
              value={onlyTitle ? 2 : matchType}
              disabled={onlyTitle}
              onChange={(value) => {setMatchType(value)}}
            >
              <Option value={1}>精准检索</Option>
              <Option value={2}>模糊检索</Option>
            </Select>
          }
          allowClear
          onClear={() => {}}
          size={'large'}
          // style={{ width: 880 }}
          placeholder={matchType === 1 ? 
            "请输入单个关键词，如需检索多个关键词请用高级检索" : 
            "请输入关键词，多关键词则以空格分隔"
          }
          onSearch={onSearch}
          //占位符 
          enterButton=" 搜    索 "
        />



        </ConfigProvider>
        <Checkbox
          //当复选框状态改变时，调用setOnlyTitle函数，并将复选框的`checked属性作为参数传入
          onChange={(e) => {
            setOnlyTitle(e.target.checked);
            if(e.target.checked) {
              setMatchType(2);
            }
          }}
          style={{ marginLeft: 6 ,fontSize: 16}}
        >仅标题</Checkbox>
        <a style={{ fontSize: 16, color: 'blue',marginRight:10 }} onClick={() => setOpen(true)}>高级搜索</a>
      </div>
      <ModalForm
        title="高级搜索"
        //初始值只能在这里设置；只有设置了ProFormList的初始值，才会默认出现一组检索条件的效果
        initialValues={{ logic: 'and', searchType: 2}}
        formRef={formRef}
        width={1080}
        open={open}
        onOpenChange={setOpen}
        submitter={{
          searchConfig: {
            resetText: '重置',
          },
          resetButtonProps: {
            onClick: () => {
              formRef.current?.resetFields();
                // setOpen(false);
            },
          },
        }}
        onFinish={async (values) => {
          console.log(values)
          setQuery(JSON.stringify(values))
          setOpen(false);
        }}
      >
        <div style={{
          margin: '10px 0 ',
          display: 'flex',
          alignItems: 'center',
          backgroundColor: '#f0f0f0',
          padding: '10px',
          borderRadius: '4px',
          fontSize: 16
        }}>
          <div>关键词满足以下</div>
          <ConfigProvider
            theme={{
              components: {
                Select: {
                  selectorBg: '#f0f0f0',
                  activeBorderColor: '#f0f0f0',
                  hoverBorderColor: '#f0f0f0',
                  colorBorder: '#f0f0f0',
                },
              },
            }}
          >
            <Form.Item
              name='searchType'
              hidden={true}
            ></Form.Item>
            <Form.Item
              name='logic'
              style={{ margin: 0 }}
              rules={[{ required: true }]}
            >
              <Select
                style={{ width: 70, marginRight: 10, marginLeft: 10, fontSize: 18 }}
                placeholder={'关系'}
                options={[
                  { value: 'and', label: '全部' },
                  { value: 'or', label: '任一' },
                ]}
              />
            </Form.Item>
          </ConfigProvider>
          <div>条件</div>
        </div>

        <ProFormList
          creatorButtonProps={{
            position: 'bottom',
            creatorButtonText: '添加一组检索条件',
          }}
          style={{ marginTop: 10 }}
          // min={1}
          max={6}
          name='condition'
          itemContainerRender={(doms) => {
            return <ProForm.Group style={{ width: 980 }}>{doms}</ProForm.Group>;
          }}
        >
          {(f, index, action) => {
            return (
              <>
                <ProFormSelect
                  name="range"
                  valueEnum={{
                    content: '全文',
                    title: '标题',
                  }}
                  placeholder="范围"
                  width={70}
                />
                <ProFormSelect
                  rules={[{ required: true }]}
                  name="include"
                  width={90}
                  valueEnum={{
                    contain: '包含',
                    notContain: '不包含',
                    orContain: '或包含',
                  }}
                  placeholder="包含关系"
                />
                <ProFormDependency name={['include','range']}>
                  {({ include, range }) => {
                    if (include === 'contain' && range === 'content') {
                      return (
                        <ProFormSelect
                          rules={[{ required: true }]}
                          name="keywordRange"
                          width={70}
                          valueEnum={{
                            normal: '常规',
                            sentence: '同句',
                            paragraphs: '同段',
                            span: '间隔',
                          }}
                          placeholder="位置"
                        />
                      );
                    }
                    return (
                      <ProFormSelect
                        name="keywordRange"
                        width={70}
                        // disabled={true}
                        valueEnum={{
                          normal: '常规',
                          // sentence: '同句',
                          // paragraphs: '同段',
                          // span: '间隔',
                        }}
                        placeholder="位置"
                      />
                    )
                  }}
                </ProFormDependency>
                <ProFormDependency name={['keywordRange']}>
                  {({ keywordRange }) => {
                    if (keywordRange === 'span' || keywordRange === 'sentence' || keywordRange === 'paragraphs') {
                      return (
                          <ProFormDigit
                            rules={[{ required: true }]}
                            name="slop"
                            width={60}
                            min={1}
                            max={100}
                            fieldProps={{ precision: 0 }}
                          />
                      );
                    }
                    return null;
                  }}
                </ProFormDependency>
                <ProFormDependency name={['keywordRange']}>
                  {({ keywordRange }) => {
                    if (keywordRange === 'span' || keywordRange === 'sentence' || keywordRange === 'paragraphs') {
                      return (
                          // 设置slop最大间隔
                          <ProFormSelect
                            name="order"
                            width={70}
                            valueEnum={{
                              true: '是',
                              false: '否'
                            }}
                            placeholder="顺序"
                          />
                      );
                    }
                    return null;
                  }}
                </ProFormDependency>
                <ProFormText
                  rules={[{ required: true }]}
                  name="text"
                  width={420}
                  placeholder="请输入关键词，多关键词则以空格分隔"
                  allowClear
                />
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'flex-end',
                    gap: '8px',
                    height: 60,
                  }}
                >
                </div>
              </>
            );
          }}
        </ProFormList>
        <Card style={{backgroundColor: 'rgba(242,240,240,0.29)',fontSize: 14, padding: '3px 3px 0 3px '}}>
          <div style={{fontSize:16,fontWeight:600,marginBottom:6}}>注意</div>
          <p>1. 检索结果的精准度很大程度取决于文档OCR文字识别精准与否。</p>
          <p>2. 设置间隔条件时，应当输入两个及以上关键词，默认关键词按顺序检索。</p>
          <p>3. 鉴于目前OCR及算法限制，暂未处理同句、同段跨页情形，故无法检索出跨页的同句、同段。</p>
        </Card>
      </ModalForm>
    </>

  );
}

export default CustomSearchBox;