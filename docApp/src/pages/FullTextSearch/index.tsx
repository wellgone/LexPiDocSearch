import Searchkit, { ElasticsearchResponseBody, SearchRequest } from 'searchkit';
import Client from '@searchkit/instantsearch-client'
import 'instantsearch.css/themes/satellite-min.css';
import './index.css'
import CustomCurrentRefinements from './components/CustomCurrentRefinements';
import {
  InstantSearch,
  Configure,
} from 'react-instantsearch';

import { Button, Card, Col, Drawer, Empty, message, Row, Tooltip } from 'antd';
import React, { useState, useEffect } from 'react';
import { FilterOutlined, ExclamationCircleOutlined } from '@ant-design/icons';

import DocRefinementList from './components/DocRefinementList';
import CustomSearchBox from './components/CustomSearchBox';
import CustomStats from './components/CustomStats';
import { ProCard } from "@ant-design/pro-components";
import PDFView from '@/components/PDFView';
import { getPreviewUrlByFileName } from "@/services/doc";
import CustomRefinementList from "./components/CustomRefinementList";
import CustomPagination from "./components/CustomPagination";
import CustomHits from './components/CustomHits';
import CustomHierarchicalMenu from './components/CustomHierarchicalMenu';
import AIAssistant from '@/components/AIAssistant';
import { createNote } from '../Note/service';
import ContextMenu from './components/ContextMenu';

// 获取环境变量，带默认值
const getEnvVar = (key: keyof ImportMetaEnv, defaultValue: string = ''): string => {
  return import.meta.env?.[key] || defaultValue;
};

const generateSpanTerm = (textArr: string[]) => {
  return textArr.map(text => ({
    span_term: {
      section_text: {
        value: text,
      },
    },
  }));
};

// 类型选项
const typeOptions = [
  { value: 1, label: '图书' },
  { value: 2, label: '自定义PDF' },
];

const sk = new Searchkit({
  connection: {
    host: '/es',  // 使用代理地址
    auth: {
      username: getEnvVar('VITE_ES_USERNAME', 'elastic'),
      password: getEnvVar('VITE_ES_PASSWORD', 'wellgone'),
    },
  },
  search_settings: {
    search_attributes: [{ field: 'section_text', weight: 1 },{ field: 'book_title', weight: 2 }],
    result_attributes: ['id', 'book_id', 'book_title', 'author', 'publisher', 'publication_year', 'page_num','pic_path', 'file_name', 'isbn','section_text','topicLevels','tags','opac_series','series','type','category'],
    highlight_attributes: ['book_title','book_title.keyword','section_text'],
    snippet_attributes: ['book_title','section_text'],
    facet_attributes: [
      { attribute: 'publisher', field: 'publisher', type: 'string' }, 
      { attribute: 'category', field: 'category', type: 'string' },
      { attribute: 'tags', field: 'tags.keyword', type: 'string' },
      { attribute: 'opac_series', field: 'opac_series.keyword', type: 'string' },
      { attribute: 'series', field: 'series.keyword', type: 'string' }, 
      // { attribute: 'topics_lvl0', field: 'lvl0', type: 'string',nestedPath: 'topicLevels'},
      // { attribute: 'topics_lvl1', field: 'lvl1', type: 'string',nestedPath: 'topicLevels'},
      // { attribute: 'topics_lvl2', field: 'lvl2', type: 'string',nestedPath: 'topicLevels'},
      // { attribute: 'topics_lvl3', field: 'lvl3', type: 'string',nestedPath: 'topicLevels'},
      // { attribute: 'topics_lvl4', field: 'lvl4', type: 'string',nestedPath: 'topicLevels'},
      { attribute: 'topics_lvl0', field: 'topicLevels.lvl0.keyword', type: 'string'},
      { attribute: 'topics_lvl1', field: 'topicLevels.lvl1.keyword', type: 'string'},
      { attribute: 'topics_lvl2', field: 'topicLevels.lvl2.keyword', type: 'string'},
      { attribute: 'topics_lvl3', field: 'topicLevels.lvl3.keyword', type: 'string'},
      { attribute: 'topics_lvl4', field: 'topicLevels.lvl4.keyword', type: 'string'},
      { attribute: 'author', type: 'string', field: 'author' },
      { attribute: 'publication_year', type: 'string', field: 'publication_year' },
      { attribute: 'type', type: 'string', field: 'type' },
      {attribute: 'book_title', type: 'string', field: 'book_title.keyword'},
    ],
  },
});

//自定义检索条件
//1.判断searchType 1为普通检索，2为高级检索
//2.处理searchType 1 普通检索
//2.1 获取查询信息-
//3.处理searchType 2 高级检索
const searchClient = Client(sk, {
  getQuery: (query: string) => {
    if (query !== '') {
      const queryJson = JSON.parse(query);
      if (queryJson.searchType === 1) {
        const { onlyTitle, queryText, matchType } = queryJson.queryData;

        if (matchType === 1) {
          return [{
            'bool': {
              'should': [
                {
                  'match_phrase': {
                    'book_title': {
                      'query': queryText,
                      'slop': 0
                    }
                  }
                },
                {
                  'match_phrase': {
                    'section_text': {
                      'query': queryText,
                      'slop': 0
                    }
                  }
                }
              ],
              'minimum_should_match': 1
            }
          }];
        } else {
          if (onlyTitle) {
            return [{
              'bool': {
                'must': [
                  {
                    'match': {
                      'book_title': {
                        'query': queryText,
                        'operator': 'and'
                      }
                    }
                  }
                ]
              }
            }];
          } else {
            return [{
              'bool': {
                'should': [
                  {
                    'match': {
                      'book_title': {
                        'query': queryText,
                        'operator': 'and',
                        'boost': 2
                      }
                    }
                  },
                  {
                    'match': {
                      'section_text': {
                        'query': queryText,
                        'operator': 'and'
                      }
                    }
                  }
                ],
                'minimum_should_match': 1
              }
            }];
          }
        }
      } else if (queryJson.searchType === 2) {
        const logic = queryJson.logic;
        const condition = queryJson.condition;
        const advQuery = queryJson.query;
        if (advQuery !== undefined) {
          const parse = JSON.parse(advQuery);
          return [parse];
        }

        const processedConditions = condition.map((item: any) => {
          const { include, keywordRange, text, range } = item;
          const itemField = range === 'content' ? 'section_text' : 'book_title';

          if (keywordRange === 'normal') {
            const keywords = text.split(" ").filter(Boolean);
            const keywordQueries = keywords.map((keyword: string) => ({
              'match_phrase': {
                [itemField]: {
                  'query': keyword,
                  'slop': 0
                }
              }
            }));

            if (include === 'contain') {
              return {
                'bool': {
                  'must': keywordQueries
                }
              };
            } else if (include === 'notContain') {
              return {
                'bool': {
                  'must_not': keywordQueries
                }
              };
            } else {
              return {
                'bool': {
                  'should': keywordQueries,
                  'minimum_should_match': 1
                }
              };
            }
          } else if (keywordRange === 'sentence') {
            const spanTerm = generateSpanTerm(text.split(" "));
            return {
              'bool': {
                [include === 'contain' ? 'must' : include === 'notContain' ? 'must_not' : 'should']: [{
                  'span_not': {
                    'include': {
                      'span_near': {
                        'clauses': spanTerm,
                        'slop': item.slop,
                        'in_order': item.order
                      }
                    },
                    'exclude': {
                      'span_term': {
                        'section_text': {
                          'value': "。/？/！"
                        }
                      }
                    }
                  }
                }]
              }
            };
          } else if (keywordRange === 'paragraphs') {
            const spanTerm = generateSpanTerm(text.split(" "));
            return {
              'bool': {
                [include === 'contain' ? 'must' : include === 'notContain' ? 'must_not' : 'should']: [{
                  'span_not': {
                    'include': {
                      'span_near': {
                        'clauses': spanTerm,
                        'slop': item.slop,
                        'in_order': item.order
                      }
                    },
                    'exclude': {
                      'span_term': {
                        'section_text': {
                          'value': "\\n"
                        }
                      }
                    }
                  }
                }]
              }
            };
          } else if (keywordRange === 'span') {
            const spanTerm = generateSpanTerm(text.split(" "));
            return {
              'bool': {
                [include === 'contain' ? 'must' : include === 'notContain' ? 'must_not' : 'should']: [{
                  'span_near': {
                    'clauses': spanTerm,
                    'slop': item.slop,
                    'in_order': item.order
                  }
                }]
              }
            };
          }
          
          // 默认返回空查询
          return {
            match_all: {}
          };
        });

        if (condition.length > 1) {
          return [{
            'bool': {
              [logic === 'and' ? 'must' : 'should']: processedConditions
            }
          }];
        } else {
          return processedConditions;
        }
      } else {
        console.log("未知的查询类型")
        return false;
      }
    }

    return [{
      "match_all": {}
    }];
  },
  hooks: {
    beforeSearch: async (searchRequests:any) => {
      // console.log("searchRequests",searchRequests)
      return searchRequests
    },
    afterSearch: async (requests: SearchRequest[], responses: ElasticsearchResponseBody[]) => {
        //如果response不为空，则进行处理
        if (responses) {
          responses[0].hits.hits.forEach(hits => {
            //如果存在hightlight且section_text不为空，则进行处理
            if (hits.highlight && hits.highlight?.section_text) {
              //将hit.highlight中的section_text数组整合成一个字符串,用@=||=@分隔，然后存回去;如果book_title字段存在且不为空，则将book_title数组直接存回去
              hits.highlight = { section_text: [hits.highlight?.section_text.join('@=||=@') || ''], book_title: hits.highlight?.book_title || '' };
            }
          });
        }
      // });
      return responses;
    }
}
});

// const searchClient = Client(sk);



const FullTextSearch = () => {
  const [currentKeyWord, setCurrentKeyWord] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [currentFileUrl, setCurrentFileUrl] = useState(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [userId, setUserId] = useState<string>('');
  const [contextMenu, setContextMenu] = useState({
    visible: false,
    x: 0,
    y: 0,
    selectedText: '',
  });

  const [currentDocInfo, setCurrentDocInfo] = useState({
    title: '',
    publisher: '',
    publicationYear: '',
    author: '',
    pageNum: 0,
  });

    // 初始化时获取用户ID
    useEffect(() => {
      const storedUserId = localStorage.getItem('userId');
      if (!storedUserId) {
        message.error('获取用户信息失败，请重新登录');
        window.location.href = '/user/login';
        return;
      }
      setUserId(storedUserId);
    }, []);
    
  // 当currentFileUrl改变时获取预览地址
  useEffect(() => {
    const fetchPreviewUrl = async () => {
      if (currentFileUrl) {
        try {
          const response = await getPreviewUrlByFileName(currentFileUrl);
          if (response.code === 0 && response.data) {
            setPreviewUrl(response.data);
          }
        } catch (error) {
          console.error('获取预览地址失败:', error);
        }
      }
    };
    fetchPreviewUrl();
  }, [currentFileUrl]);

  const handleHitClick = (hit: any) => {
    //当图书链接与当前选中项不一致时，设置fileUrl
    if (currentFileUrl !== hit.file_name) {
      setCurrentFileUrl(hit.file_name);
    }
    //设定关键词
    const snippet = hit._snippetResult?.section_text?.matchedWords;
    //如果没有进行检索过，则snippet应为空，故需排除
    if (snippet) {
      //去重
      const snippetSet = new Set(snippet);
      setCurrentKeyWord(Array.from(snippetSet).join(' '))
    }
    //设定指定页,需将page_num转换为int
    setCurrentPage(parseInt(hit.page_num))

    // 更新当前图书信息
    setCurrentDocInfo({
      title: hit.book_title,
      publisher: hit.publisher,
      publicationYear: hit.publication_year,
      author: hit.author,
      pageNum: parseInt(hit.page_num),
    });
  };

  
  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    const selection = window.getSelection();
    const selectedText = selection?.toString().trim();

    if (selectedText) {
      setContextMenu({
        visible: true,
        x: e.clientX,
        y: e.clientY,
        selectedText,
      });
    }
  };

  const handleAddToNote = async (text: string, docInfo: any,reportId:number) => {
    try {
      const noteData = {
        note: {
          content: text,
          sourceName: docInfo.title,
          sourcePress: docInfo.publisher,
          sourcePublicationDate: docInfo.publicationYear ? new Date(docInfo.publicationYear).toISOString().split('T')[0] : undefined,
          sourceAuthor: docInfo.author,
          sourcePageSize: docInfo.pageNum,
          orderNum: 9999,
          createTime: new Date().toISOString(),
          modifiedTime: new Date().toISOString(),
          userId: userId, 
          parentId: 0,
        },
        reportId: reportId
      };

      const response = await createNote(noteData);
      if (response.code === 0) {
        message.success('笔记添加成功');
      } else {
        message.error(response.message || '笔记添加失败');
      }
    } catch (error) {
      message.error('笔记添加失败');
      console.error('添加笔记失败:', error);
    }
  };

  const handleCloseContextMenu = () => {
    setContextMenu({ ...contextMenu, visible: false });
  };

  useEffect(() => {
    document.addEventListener('click', handleCloseContextMenu);
    return () => {
      document.removeEventListener('click', handleCloseContextMenu);
    };
  }, []);

  return (
    <>
      <Row justify="center">
        <Col span={15}>
          <InstantSearch
            indexName='books'
            searchClient={searchClient}
          >
            <Configure
              analytics={false}
              hitsPerPage={8}
            />
            <CustomSearchBox placeholder={'检索关键词'}/>
            
            <Card style={{ margin: '5px 5px 0px 5px'}} bodyStyle={{padding: '15px'}}>
              <Row align="middle" justify="space-between">
                <Col>
                  <Button 
                    color="default" 
                    variant="text"
                    icon={<FilterOutlined />}
                    onClick={() => setDrawerVisible(true)}
                    style={{ marginRight: 16 }}
                  >
                    属性过滤
                  </Button>
                </Col>
                <Col flex="auto" style={{ textAlign: 'right' }}>
                  <CustomStats />
                </Col>
              </Row>
              <Row>
                <Col flex="auto">
                  <CustomCurrentRefinements />
                </Col>
              </Row>
            </Card>

            <Drawer
              title="属性过滤"
              placement="left"
              width={500}
              onClose={() => setDrawerVisible(false)}
              open={drawerVisible}
            >
              <ProCard
                tabs={{
                  tabBarExtraContent: 
                    <Tooltip title="注意！属性过滤中的检索非模糊检索，而系从首字开始的精准匹配。">
                      <ExclamationCircleOutlined style={{marginRight: 20, fontWeight:600}} />
                    </Tooltip>
                }}
              >
                <ProCard.TabPane key="tab1" tab="出版社">
                  <CustomRefinementList attribute="publisher" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab2" tab="作者">
                  <CustomRefinementList attribute="author" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab3" tab="出版年份">
                  <CustomRefinementList attribute="publication_year" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab4" tab="标签">
                  <CustomRefinementList attribute="tags" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab5" tab="系列">
                  <CustomRefinementList attribute="series" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab6" tab="分类">
                  <CustomRefinementList attribute="category" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab7" tab="文档类型">
                  <CustomRefinementList attribute="type" searchable showMore options={typeOptions} />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab8" tab="OPAC系列">
                  <CustomRefinementList attribute="opac_series" searchable showMore />
                </ProCard.TabPane>
                <ProCard.TabPane key="tab9" tab="主题分类">
                  <CustomHierarchicalMenu
                    attributes={[
                      'topics_lvl0',
                      'topics_lvl1',
                      'topics_lvl2',
                      'topics_lvl3',
                      'topics_lvl4'
                    ]}
                  />
                </ProCard.TabPane>
              </ProCard>
            </Drawer>

            <Row>
              <Col span={9}>
                <Card style={{margin:'5px',height:'780px'}}>
                  <DocRefinementList
                    attribute="book_title"
                    showMoreLimit={100}
                    searchable={true}
                    showMore={true}
                  />
                </Card>
              </Col>
              <Col span={15}>
                <Card style={{margin:'5px', height:'780px'}} styles={{ body: { paddingTop: 10 } }}>
                  <div style={{height: '710px', overflow: 'auto' }}>
                    <CustomHits
                      onHitClick={handleHitClick}
                      currentKeyWord={currentKeyWord}
                    />
                  </div>
                  <div style={{justifyContent: 'center', display: 'flex'}}>
                    <CustomPagination />
                  </div>
                </Card>
              </Col>
            </Row>
          </InstantSearch>
        </Col>
        <Col span={9}>
          {currentFileUrl !== null && previewUrl ? (
            <div 
              style={{ height: '755px', width: '650px' }}
              onContextMenu={handleContextMenu}
            >
              <PDFView
                file={'/api3/'+previewUrl}
                searchText={currentKeyWord}
                pageNum={currentPage}
              />
              <ContextMenu
                visible={contextMenu.visible}
                x={contextMenu.x}
                y={contextMenu.y}
                userId={userId}
                selectedText={contextMenu.selectedText}
                docInfo={currentDocInfo}
                onClose={handleCloseContextMenu}
                onAddToNote={handleAddToNote}
              />
            </div>
          ) : (
            <Empty 
              description="请选择图书"
            />
          )}
        </Col>
      </Row>
      <AIAssistant />
    </>
  );
};

export default FullTextSearch;
