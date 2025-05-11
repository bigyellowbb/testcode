package com.example.demo.service.impl;

import com.example.demo.model.PreData;
import com.example.demo.model.PredictionResult;
import com.example.demo.repository.PreDataRepository;
import com.example.demo.repository.PredictionResultRepository;
import com.example.demo.service.PredictionService;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionServiceImpl implements PredictionService {

    private static final int KIM = 5; // 滑动窗口大小
    private static final int ZIM = 1; // 预测步长
    private static final int BATCH_SIZE = 32;
    private static final int EPOCHS = 70;

    private final PreDataRepository preDataRepository;
    private final PredictionResultRepository resultRepository;

    public PredictionServiceImpl(PreDataRepository preDataRepository,
                                 PredictionResultRepository resultRepository) {
        this.preDataRepository = preDataRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    public List<Map<String, Double>> getPredictionDataFromDB() {
        List<PreData> dataList = preDataRepository.findAll();

        return dataList.stream()
                .map(data -> Map.of(
                        "water_level", data.getWaterLevel(),
                        "sand_content", data.getSandContent(),
                        "dew_point", data.getDewPoint(),
                        "internet_traffic", data.getInternetTraffic()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void trainAndPredict(List<Double> originalData) {
        // 数据校验
        if (originalData.size() < KIM + ZIM) {
            throw new IllegalArgumentException("需要至少 " + (KIM + ZIM) + " 个数据点");
        }

        // 数据预处理
        List<double[]> dataset = createSlidingWindows(originalData);

        // 数据集划分
        int trainSize = (int) (dataset.size() * 0.8);
        List<double[]> trainData = dataset.subList(0, trainSize);
        List<double[]> testData = dataset.subList(trainSize, dataset.size());

        // 转换训练数据
        DataSet trainingDataSet = convertToDataSet(trainData, KIM);

        // 数据归一化
        NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler(0, 1);
        normalizer.fit(trainingDataSet);
        normalizer.transform(trainingDataSet);

        // 构建LSTM模型
        MultiLayerNetwork net = buildLSTMNetwork();

        // 添加监听器以便观察训练过程
        net.setListeners(new ScoreIterationListener(10));

        // 模型训练
        trainModel(net, trainingDataSet);

        // 处理测试数据
        INDArray testFeatures = prepareTestFeatures(testData, KIM);
        DataSet testDataSet = new DataSet(testFeatures, null);
        normalizer.transform(testDataSet);

        // 保存预测结果
        savePredictions(net, normalizer, trainingDataSet.getFeatures(), PredictionResult.PredictionType.TRAIN);
        savePredictions(net, normalizer, testDataSet.getFeatures(), PredictionResult.PredictionType.TEST);
    }

    private List<double[]> createSlidingWindows(List<Double> data) {
        List<double[]> windows = new ArrayList<>();
        for (int i = 0; i <= data.size() - KIM - ZIM; i++) {
            double[] window = new double[KIM + 1];
            System.arraycopy(data.subList(i, i + KIM).stream().mapToDouble(Double::doubleValue).toArray(),
                    0, window, 0, KIM);
            window[KIM] = data.get(i + KIM + ZIM - 1);
            windows.add(window);
        }
        return windows;
    }

    private DataSet convertToDataSet(List<double[]> data, int featureDim) {
        INDArray features = Nd4j.create(data.size(), featureDim);
        INDArray labels = Nd4j.create(data.size(), 1);

        for (int i = 0; i < data.size(); i++) {
            double[] window = data.get(i);
            features.putRow(i, Nd4j.create(window, new int[]{0, featureDim}));
            labels.putScalar(i, window[featureDim]);
        }
        return new DataSet(features, labels);
    }

    private MultiLayerNetwork buildLSTMNetwork() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .updater(new Adam(0.01))
                .l2(1e-7)
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(new LSTM.Builder()
                        .nIn(KIM)
                        .nOut(70)
                        .activation(Activation.TANH)
                        .build())
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(70)
                        .nOut(1)
                        .build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(config);
        net.init();
        return net;
    }

    private void trainModel(MultiLayerNetwork net, DataSet dataSet) {
        ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<>(
                Collections.singletonList(dataSet),
                BATCH_SIZE
        );
        net.fit(iterator, EPOCHS);
    }

    private INDArray prepareTestFeatures(List<double[]> testData, int featureDim) {
        INDArray features = Nd4j.create(testData.size(), featureDim);
        for (int i = 0; i < testData.size(); i++) {
            double[] window = testData.get(i);
            features.putRow(i, Nd4j.create(window, new int[]{0, featureDim}));
        }
        return features;
    }

    private void savePredictions(MultiLayerNetwork net,
                                 NormalizerMinMaxScaler normalizer,
                                 INDArray features,
                                 PredictionResult.PredictionType type) {
        // 1. 特征归一化
        DataSet tempSet = new DataSet(features, null);
        normalizer.transform(tempSet); // 修改内部数据

        // 2. 进行预测
        INDArray predictions = net.output(tempSet.getFeatures());

        // 3. 反归一化处理
        normalizer.revertFeatures(predictions);

        // 4. 保存结果
        List<PredictionResult> results = new ArrayList<>();
        for (int i = 0; i < predictions.rows(); i++) {
            PredictionResult result = new PredictionResult();
            result.setType(type);
            result.setPredictedValue(predictions.getDouble(i, 0)); // 假设预测结果在第0列
            results.add(result);
        }
        resultRepository.saveAll(results);
    }
}
//package com.example.demo.service.impl;
//
//import com.example.demo.model.PreData;
//import com.example.demo.model.PredictionResult;
//import com.example.demo.repository.PreDataRepository;
//import com.example.demo.repository.PredictionResultRepository;
//import com.example.demo.service.PredictionService;
//import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
//import org.deeplearning4j.nn.conf.layers.LSTM;
//import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
//import org.deeplearning4j.nn.weights.WeightInit;
//import org.nd4j.linalg.activations.Activation;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.dataset.DataSet;
//import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
//import org.nd4j.linalg.factory.Nd4j;
//import org.nd4j.linalg.learning.config.Adam;
//import org.nd4j.linalg.lossfunctions.LossFunctions;
//import org.springframework.stereotype.Service;
//import java.util.List;
//import java.util.Map;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.stream.Collectors;
//
//@Service
//public class PredictionServiceImpl implements PredictionService {
//
//    private static final int KIM = 5; // 滑动窗口大小
//    private static final int ZIM = 1; // 预测步长
//    private static final int BATCH_SIZE = 32;
//    private static final int EPOCHS = 70;
//
//    // 新增 PreDataRepository 依赖
//    private final PreDataRepository preDataRepository;
//    private final PredictionResultRepository resultRepository;
//
//    // 修改构造函数注入
//    public PredictionServiceImpl(PreDataRepository preDataRepository,
//                                 PredictionResultRepository resultRepository) {
//        this.preDataRepository = preDataRepository;
//        this.resultRepository = resultRepository;
//    }
//
//    // 新增数据获取方法实现
//    @Override
//    public List<Map<String, Double>> getPredictionDataFromDB() {
//        List<PreData> dataList = preDataRepository.findAll();
//
//        return dataList.stream()
//                .map(data -> Map.of(
//                        "water_level", data.getWaterLevel(),
//                        "sand_content", data.getSandContent(),
//                        "dew_point", data.getDewPoint(),
//                        "internet_traffic", data.getInternetTraffic()
//                ))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public void trainAndPredict(List<Double> originalData) {
//        // 数据校验
//        if (originalData.size() < KIM + ZIM) {
//            throw new IllegalArgumentException("需要至少 " + (KIM + ZIM) + " 个数据点");
//        }
//
//        // 数据预处理
//        List<double[]> dataset = createSlidingWindows(originalData);
//
//        // 数据集划分
//        int trainSize = (int) (dataset.size() * 0.8);
//        List<double[]> trainData = dataset.subList(0, trainSize);
//        List<double[]> testData = dataset.subList(trainSize, dataset.size());
//
//        // 转换训练数据
//        DataSet trainingDataSet = convertToDataSet(trainData);
//
//        // 数据归一化
//        NormalizerStandardize normalizer = new NormalizerStandardize();
//        normalizer.fitLabel(true);
//        normalizer.fit(trainingDataSet);
//        normalizer.transform(trainingDataSet);
//
//        // 构建LSTM模型
//        MultiLayerNetwork net = buildLSTMNetwork();
//
//        // 模型训练
//        trainModel(net, trainingDataSet);
//
//        // 处理测试数据
//        INDArray testFeatures = prepareTestFeatures(testData);
//        DataSet testDataSet = new DataSet(testFeatures, null);
//        normalizer.transform(testDataSet);
//
//        // 保存预测结果
//        savePredictions(net, normalizer, trainingDataSet.getFeatures(), PredictionResult.PredictionType.TRAIN);
//        savePredictions(net, normalizer, testDataSet.getFeatures(), PredictionResult.PredictionType.TEST);
//    }
//
//    private List<double[]> createSlidingWindows(List<Double> data) {
//        List<double[]> windows = new ArrayList<>();
//        for (int i = 0; i <= data.size() - KIM - ZIM; i++) {
//            double[] window = new double[KIM + 1];
//            System.arraycopy(data.subList(i, i + KIM).stream().mapToDouble(Double::doubleValue).toArray(),
//                    0, window, 0, KIM);
//            window[KIM] = data.get(i + KIM + ZIM - 1);
//            windows.add(window);
//        }
//        return windows;
//    }
//
//    private DataSet convertToDataSet(List<double[]> data) {
//        INDArray features = Nd4j.create(data.size(), KIM);
//        INDArray labels = Nd4j.create(data.size(), 1);
//
//        for (int i = 0; i < data.size(); i++) {
//            double[] window = data.get(i);
//            features.putRow(i, Nd4j.create(window, new int[]{KIM}));
//            labels.putScalar(i, window[KIM]);
//        }
//        return new DataSet(features, labels);
//    }
//
//    private MultiLayerNetwork buildLSTMNetwork() {
//        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
//                .seed(12345)
//                .updater(new Adam(0.01))
//                .l2(1e-7)
//                .weightInit(WeightInit.XAVIER)
//                .list()
//                .layer(new LSTM.Builder()
//                        .nIn(KIM)
//                        .nOut(70)
//                        .activation(Activation.TANH)  // 修正为更适合LSTM的激活函数
//                        .build())
//                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
//                        .activation(Activation.IDENTITY)
//                        .nIn(70)
//                        .nOut(1)
//                        .build())
//                .build();
//
//        MultiLayerNetwork net = new MultiLayerNetwork(config);
//        net.init();
//        return net;
//    }
//
//    private void trainModel(MultiLayerNetwork net, DataSet dataSet) {
//        ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<>(
//                Collections.singletonList(dataSet),
//                BATCH_SIZE
//        );
//        net.fit(iterator, EPOCHS);
//    }
//
//    private INDArray prepareTestFeatures(List<double[]> testData) {
//        INDArray features = Nd4j.create(testData.size(), KIM);
//        for (int i = 0; i < testData.size(); i++) {
//            double[] window = testData.get(i);
//            features.putRow(i, Nd4j.create(window, new int[]{KIM}));
//        }
//        return features;
//    }
//
//    private void savePredictions(MultiLayerNetwork net,
//                                 NormalizerStandardize normalizer,
//                                 INDArray features,
//                                 PredictionResult.PredictionType type) {
//        // 1. 特征归一化
//        DataSet tempSet = new DataSet(features, null);
//        normalizer.transform(tempSet); // 修改内部数据
//
//        // 2. 进行预测
//        INDArray predictions = net.output(tempSet.getFeatures());
//
//        // 3. 创建包含预测结果的DataSet
//        DataSet predictionSet = new DataSet(null, predictions.reshape(-1, 1));
//
//        // 4. 反归一化处理（关键修正）
//        normalizer.revertLabels((INDArray) predictionSet); // 正确调用标签反归一化
//        INDArray denormalized = predictionSet.getLabels();
//
//        // 5. 保存结果
//        List<PredictionResult> results = new ArrayList<>();
//        for (int i = 0; i < denormalized.length(); i++) {
//            PredictionResult result = new PredictionResult();
//            result.setType(type);
//            result.setPredictedValue(denormalized.getDouble(i));
//            results.add(result);
//        }
//        resultRepository.saveAll(results);
//    }
//
//}