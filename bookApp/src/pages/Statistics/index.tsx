import React, { useEffect, useState } from 'react';
import { Card, Col, Row, Typography, Spin, Progress, Divider } from 'antd';
import { getStatistics, getBookCountByCategory, getBookCountBySeries } from '@/services/statistics';
import {
  BookOutlined,
  FileOutlined,
  EditOutlined,
  ReadOutlined,
  FileSearchOutlined,
  RadarChartOutlined,
  FilePdfOutlined,
} from '@ant-design/icons';
import styles from './index.less';
import { PageContainer } from '@ant-design/pro-layout';
// import { useIntl } from 'umi';
import { Pie, WordCloud } from '@ant-design/charts';
import { position } from 'html2canvas/dist/types/css/property-descriptors/position';

const { Title, Text } = Typography;

interface StatCardProps {
  title: string;
  value: number;
  suffix: string;
  icon: React.ReactNode;
  color: string;
  bgColor: string;
  secondaryValue?: string | number | null;
  secondaryText?: string | null;
}

interface StatisticsData {
  bookCount: number;
  indexedBookCount: number;
  reportCount: number;
  noteCount: number;
  documentCount: number;
  fileCount: number;
}

interface CategoryData {
  type: string;
  value: number;
}

interface SeriesData {
  text: string;
  value: number;
  name: string;
}

const StatisticsPage: React.FC = () => {
  // const intl = useIntl();
  const [loading, setLoading] = useState<boolean>(true);
  const [stats, setStats] = useState<StatisticsData>({
    bookCount: 0,
    indexedBookCount: 0,
    reportCount: 0,
    noteCount: 0,
    documentCount: 0,
    fileCount: 0,
  });
  const [categoryData, setCategoryData] = useState<CategoryData[]>([]);
  const [seriesData, setSeriesData] = useState<SeriesData[]>([]);
  const [categoryLoading, setCategoryLoading] = useState<boolean>(true);
  const [seriesLoading, setSeriesLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const result = await getStatistics();
        if (result.code === 0) {
          setStats(result.data);
        }
      } catch (error) {
        console.error('获取统计数据失败', error);
      } finally {
        setLoading(false);
      }
    };

    const fetchCategoryData = async () => {
      try {
        const result = await getBookCountByCategory();
        console.log('分类数据结果:', result);
        if (result.code === 0) {
          const formattedData = result.data.map((item: any) => ({
            type: item.name || '未分类',
            value: parseInt(item.value, 10)
          }));
          console.log('格式化后的分类数据:', formattedData);
          setCategoryData(formattedData);
        }
      } catch (error) {
        console.error('获取分类统计数据失败', error);
      } finally {
        setCategoryLoading(false);
      }
    };

    const fetchSeriesData = async () => {
      try {
        const result = await getBookCountBySeries();
        console.log('丛书数据结果:', result);
        if (result.code === 0) {
          const formattedData = result.data.map((item: any) => ({
            text: item.name || '未分类',
            value: parseInt(item.value, 10),
            name: item.name || '未分类'
          }));
          console.log('格式化后的丛书数据:', formattedData);
          setSeriesData(formattedData);
        }
      } catch (error) {
        console.error('获取丛书统计数据失败', error);
      } finally {
        setSeriesLoading(false);
      }
    };

    fetchData();
    fetchCategoryData();
    fetchSeriesData();
  }, []);

  // 计算索引率
  const indexRatio = stats.fileCount > 0 
    ? Math.round((stats.indexedBookCount / stats.fileCount) * 100) 
    : 0;

  const StatCard: React.FC<StatCardProps> = ({ 
    title, 
    value, 
    suffix, 
    icon, 
    color, 
    bgColor,
    secondaryValue = null,
    secondaryText = null,
  }) => (
    <Card 
      className={styles.statCard} 
      bordered={false}
      style={{ borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
    >
      <div className={styles.cardContent}>
        <div className={styles.iconWrapper} style={{ backgroundColor: bgColor }}>
          {icon}
        </div>
        <div className={styles.statContent}>
          <Text className={styles.statTitle}>{title}</Text>
          <div className={styles.valueWrapper}>
            <Text className={styles.statValue} style={{ color }}>
              {value.toLocaleString()}
            </Text>
            <Text className={styles.statSuffix}>{suffix}</Text>
          </div>
          {secondaryValue !== null && (
            <Text className={styles.secondaryText} style={{ color }}>
              {secondaryText}: {secondaryValue}
            </Text>
          )}
        </div>
      </div>
    </Card>
  );

  // 饼图配置
  const pieConfig = {
    data: categoryData,
    angleField: 'value',
    colorField: 'type',
    radius: 0.6,
    innerRadius: 0.3,
    label: {
      text: (d: any) => `${d.type}  ${d.value}`,
      position:'spider',
    },
    legend: {
      color: {
        title: true,
        position: 'top',
        rowPadding: 5,
      },
    },
    // annotations: [
    //   {
    //     type: 'text',
    //     style: {
    //       text: '分类',
    //       x: '50%',
    //       y: '50%',
    //       textAlign: 'center',
    //       fontSize: 40,
    //       fontStyle: 'bold',
    //     },
    //   },
    // ],
  };

  // 词云图配置
  const wordCloudConfig = {
    data: seriesData,
    wordField: 'text',
    weightField: 'value',
    colorField: 'name',
    wordStyle: {
      fontFamily: 'Verdana',
      fontSize: [14, 60],
      rotation: 1,
    },
    showMaxWordCount: 100,
    padding: 2,
    shape: 'circle',
    height: 480,
    width: 480,
  };

  return (
    <PageContainer
      header={{
        title: '知识数据概览',
      }}
    >
      <Spin spinning={loading}>
        <div className={styles.statisticsContainer}>
          {/* <Title level={4} style={{ margin: '0 0 24px 0', textAlign: 'center' }}>
            系统数据概览
          </Title> */}
          
          <Row gutter={[24, 24]} style={{ marginBottom: 32 }}>
            <Col xs={24} md={12}>
              <Card 
                bordered={false} 
                className={styles.progressCard}
                style={{ borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
              >
                <div className={styles.progressContainer}>
                  <Progress
                    type="circle"
                    percent={indexRatio}
                    width={180}
                    strokeWidth={12}
                    strokeColor={{
                      '0%': '#108ee9',
                      '100%': '#52c41a',
                    }}
                    format={(percent) => (
                      <div className={styles.progressFormat}>
                        <div className={styles.progressPercent}>{percent}%</div>
                        <div className={styles.progressTitle}>书籍索引率</div>
                      </div>
                    )}
                  />
                  
                  <div className={styles.progressDetails}>
                    <div className={styles.progressDetail}>
                      <FileOutlined className={styles.progressIcon} style={{ color: '#1890ff' }} />
                      <div className={styles.progressInfo}>
                        <div className={styles.progressLabel}>文档总数</div>
                        <div className={styles.progressValue}>
                          {stats.fileCount.toLocaleString()}
                        </div>
                      </div>
                    </div>
                    
                    <Divider style={{ margin: '16px 0' }} />
                    
                    <div className={styles.progressDetail}>
                      <RadarChartOutlined className={styles.progressIcon} style={{ color: '#52c41a' }} />
                      <div className={styles.progressInfo}>
                        <div className={styles.progressLabel}>已索引文档</div>
                        <div className={styles.progressValue}>
                          {stats.indexedBookCount.toLocaleString()}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            </Col>
            
            <Col xs={24} md={12}>
              <Row gutter={[24, 24]}>
                <Col xs={24}>
                    <Row gutter={[24, 24]}>
                        <Col xs={12}>
                            <StatCard
                                title="书籍数量"
                                value={stats.bookCount}
                                suffix="本"
                                icon={<BookOutlined style={{ fontSize: 24, color: 'white' }} />}
                                color="#1890ff"
                                bgColor="#e6f7ff"
                                // secondaryValue={stats.bookCount > 0 ? `近30天: ${Math.round(stats.bookCount * 0.3)}` : '0'}
                                // secondaryText="新增书籍"
                                />
                        </Col>
                        <Col xs={12}>
                            <StatCard
                                title="其他文档"
                                value={stats.documentCount}
                                suffix="份"
                                icon={<FilePdfOutlined style={{ fontSize: 24, color: 'white' }} />}
                                color="#27ae60"
                                bgColor="#f6ffed"
                                // secondaryValue={stats.documentCount > 0 ? `近30天: ${Math.round(stats.documentCount * 0.3)}` : '0'}
                                // secondaryText="新增文档"
                            />
                        </Col>
                    </Row>
                  
                </Col>
                <Col xs={24}>
                    <Row gutter={[24, 24]}>
                        <Col xs={12}>
                            <StatCard
                                title="检索报告"
                                value={stats.reportCount}
                                suffix="份"
                                icon={<FileSearchOutlined style={{ fontSize: 24, color: 'white' }} />}
                                color="#722ed1"
                                bgColor="#f5f0ff"
                                // secondaryValue={stats.reportCount > 0 ? `近30天: ${Math.round(stats.reportCount * 0.3)}` : '0'}
                                // secondaryText="新增报告"
                            />
                        </Col>
                        <Col xs={12}>
                            <StatCard
                                title="笔记数量"
                                value={stats.noteCount}
                                suffix="条"
                                icon={<EditOutlined style={{ fontSize: 24, color: 'white' }} />}
                                color="#fa8c16"
                                bgColor="#fff7e6"
                                // secondaryValue={stats.noteCount > 0 ? `平均每书: ${(stats.noteCount / (stats.bookCount || 1)).toFixed(1)}` : '0'}
                                // secondaryText="笔记密度"
                            />
                        </Col>
                    </Row>
                  
                </Col>
                {/* <Col xs={24}>
                  <StatCard
                    title="笔记数量"
                    value={stats.noteCount}
                    suffix="条"
                    icon={<EditOutlined style={{ fontSize: 24, color: 'white' }} />}
                    color="#fa8c16"
                    bgColor="#fff7e6"
                    secondaryValue={stats.noteCount > 0 ? `平均每书: ${(stats.noteCount / (stats.bookCount || 1)).toFixed(1)}` : '0'}
                    secondaryText="笔记密度"
                  />
                </Col> */}
              </Row>
            </Col>
          </Row>
          
          {/* <Row gutter={[24, 24]}>
            <Col xs={24}>
              <Card
                title="系统使用概览"
                bordered={false}
                style={{ borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
              >
                <Row gutter={[24, 24]}>
                  <Col xs={24} sm={12} md={8} lg={6}>
                    <Progress
                      type="dashboard"
                      percent={75}
                      strokeColor="#1890ff"
                      size={130}
                      strokeWidth={10}
                      format={() => (
                        <div style={{ textAlign: 'center' }}>
                          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1890ff' }}>75%</div>
                          <div style={{ fontSize: '14px', color: '#666' }}>系统利用率</div>
                        </div>
                      )}
                    />
                  </Col>
                  <Col xs={24} sm={12} md={8} lg={6}>
                    <Progress
                      type="dashboard"
                      percent={63}
                      strokeColor="#52c41a"
                      size={130}
                      strokeWidth={10}
                      format={() => (
                        <div style={{ textAlign: 'center' }}>
                          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#52c41a' }}>63%</div>
                          <div style={{ fontSize: '14px', color: '#666' }}>活跃用户</div>
                        </div>
                      )}
                    />
                  </Col>
                  <Col xs={24} sm={12} md={8} lg={6}>
                    <Progress
                      type="dashboard"
                      percent={87}
                      strokeColor="#722ed1"
                      size={130}
                      strokeWidth={10}
                      format={() => (
                        <div style={{ textAlign: 'center' }}>
                          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#722ed1' }}>87%</div>
                          <div style={{ fontSize: '14px', color: '#666' }}>搜索准确率</div>
                        </div>
                      )}
                    />
                  </Col>
                  <Col xs={24} sm={12} md={8} lg={6}>
                    <Progress
                      type="dashboard"
                      percent={92}
                      strokeColor="#fa8c16"
                      size={130}
                      strokeWidth={10}
                      format={() => (
                        <div style={{ textAlign: 'center' }}>
                          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#fa8c16' }}>92%</div>
                          <div style={{ fontSize: '14px', color: '#666' }}>系统稳定性</div>
                        </div>
                      )}
                    />
                  </Col>
                </Row>
              </Card>
            </Col>
          </Row> */}

          <Row gutter={[24, 24]} style={{ marginTop: 12 }}>
            <Col xs={24} md={12}>
              <Card
                title="文档分类"
                bordered={false}
                style={{ borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', height: 550 }}
              >
                <div style={{ height: 490, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    {categoryLoading ? (
                    <div style={{ height: 490, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Spin />
                    </div>
                    ) : (
                    <div style={{ height: 490, display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '0 auto' }}>
                        <Pie {...pieConfig} height={480} width={480} />
                    </div>
                    )}
                </div>
                
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card
                title="丛书"
                bordered={false}
                style={{ borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', height: 550 }}
              >
                <div style={{ height: 490, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    {seriesLoading ? (
                    <div style={{ height: 490, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Spin />
                    </div>
                    ) : (
                    <div style={{ height: 490, display: 'flex', justifyContent: 'center', alignItems: 'center', margin: '0 auto' }}>
                        <WordCloud {...wordCloudConfig} />
                    </div>
                    )}
                </div>
              </Card>
            </Col>
          </Row>
        </div>
      </Spin>
    </PageContainer>
  );
};

export default StatisticsPage; 