package com.example.demo.service;

import com.example.demo.model.PredictionResult;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import java.util.List;
import java.util.Map;


public interface PredictionService {
    void trainAndPredict(List<Double> originalData);
    List<Map<String, Double>> getPredictionDataFromDB();
}
