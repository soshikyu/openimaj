package org.openimaj.ml.neuralnet;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gov.sandia.cognition.io.CSVUtility;
import gov.sandia.cognition.math.matrix.Vector;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.encog.engine.network.activation.ActivationStep;
import org.encog.ml.BasicML;
import org.encog.ml.MLInputOutput;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.specific.CSVNeuralDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.training.NEATTraining;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.CalculateScore;
import org.encog.neural.networks.training.TrainingSetScore;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.util.simple.EncogUtility;
import org.openimaj.util.pair.IndependentPair;

import Jama.Matrix;

/**
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>, Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 * Just some experiments using the sandia cognitive foundary neural nets.
 *
 */
public class HandWritingNeuralNetENCOG {
	
	/**
	 * Default location of inputs
	 */
	public static final String INPUT_LOCATION = "/org/openimaj/ml/handwriting/inputouput.csv";


	private int maxTests = 10;

	private TIntIntHashMap examples;
	private TIntObjectHashMap<List<IndependentPair<double[],double[]>>> tests;

	

	private int totalTests = 0;

	private MLRegression network;

	private MLDataSet training;
	
	/**
	 * @throws IOException
	 * Load X input and y output from {@link #INPUT_LOCATION} 
	 */
	public HandWritingNeuralNetENCOG() throws IOException {
		
		
		examples = new TIntIntHashMap();
		this.tests = new TIntObjectHashMap<List<IndependentPair<double[],double[]>>>();
		prepareDataCollection();
		learnNeuralNet();
		testNeuralNet();
//		new HandWritingInputDisplay(this.training);
	}
	
	private void testNeuralNet() {
		final double[][] xVals = new double[totalTests ][];
		final int[] yVals = new int[totalTests ];
		this.tests.forEachEntry(new TIntObjectProcedure<List<IndependentPair<double[],double[]>>>() {
			int done = 0;
			@Override
			public boolean execute(int number, List<IndependentPair<double[], double[]>> xypairs) {
				for (IndependentPair<double[], double[]> xyval: xypairs) {
					double[] guessed = network.compute(new BasicNeuralData(xyval.firstObject())).getData(); // estimate
					int maxIndex = 0;
					double maxValue = 0;
					for (int i = 0; i < guessed.length; i++) {
						if(maxValue  < guessed[i])
						{
							maxValue = guessed[i];
							maxIndex = i;
						}
					}
					xVals[done] = xyval.firstObject();
					yVals[done] = (maxIndex + 1) % 10;
					done ++;
				}
				return true;
			}
		});
		new HandWritingInputDisplay(xVals, yVals);
	}

	private void prepareDataCollection() throws IOException {
		File tmp = File.createTempFile("data", ".csv");
		InputStream stream = HandWritingNeuralNetENCOG.class.getResourceAsStream(INPUT_LOCATION);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		PrintWriter writer = new PrintWriter(new FileWriter(tmp));
		while((line=reader.readLine())!=null){
			writer.println(line);
		}
		writer.close();
		reader.close();
		training = new CSVNeuralDataSet(tmp.getAbsolutePath(), 400, 10, false);
		Iterator<MLDataPair> elementItr = this.training.iterator();
		for (; elementItr.hasNext();) {
			MLDataPair type = elementItr.next();
			double[] yData = type.getIdealArray();
			double[] xData = type.getInputArray();
			int yIndex = 0;
			while(yData[yIndex]!=1)yIndex++;
			int currentCount = this.examples.adjustOrPutValue(yIndex, 1, 1);
			if(currentCount < this.maxTests){
				
				List<IndependentPair<double[], double[]>> numberTest = this.tests.get(yIndex);
				if(numberTest == null){
					this.tests.put(yIndex, numberTest = new ArrayList<IndependentPair<double[],double[]>>());
				}
				numberTest.add(IndependentPair.pair(xData,yData));
				totalTests++;
			}
		}
		
	}

	private void learnNeuralNet() {
//		this.network = EncogUtility.simpleFeedForward(400, 100, 0, 10, false);
//		MLTrain train = new Backpropagation(this.network, this.training);
//		MLTrain train = new ResilientPropagation(this.network, this.training);
		
		MLTrain train = withNEAT();
//		MLTrain train = withResilieant();
		EncogUtility.trainToError(train, 0.01515);
		this.network = (MLRegression) train.getMethod();
	}


	private MLTrain withNEAT() {
		NEATPopulation pop = new NEATPopulation(400,10,1000);
		CalculateScore score = new TrainingSetScore(this.training);
		// train the neural network
		ActivationStep step = new ActivationStep();
		step.setCenter(0.5);
		pop.setOutputActivationFunction(step);
		MLTrain train = new NEATTraining(score, pop);
		return train;
	}
	
	private MLTrain withResilieant(){
		MLTrain train = new ResilientPropagation(EncogUtility.simpleFeedForward(400, 100, 0, 10, false), this.training);
		return train;
	}

	private static <T> ArrayList<T> toArrayList(T[] values) {
		ArrayList<T> configList = new ArrayList<T>();
		for (T t : values) {
			configList.add(t);
		}
		return configList;
	}

	private Matrix fromCSV(BufferedReader bufferedReader, int nLines) throws IOException {
		
		String[] lineValues = null;
		double[][] outArr = null;
		Matrix retMat = null;
		int row = 0;
		while ((lineValues = CSVUtility.nextNonEmptyLine(bufferedReader)) != null) {
			if(outArr == null) {
				retMat = new Matrix(nLines, lineValues.length);
				outArr = retMat.getArray();
			}
			
			for (int col = 0; col < lineValues.length; col++) {
				outArr[row][col] = Double.parseDouble(lineValues[col]);
			}
			row++;
		}
		return retMat;
	}
	
	public static void main(String[] args) throws IOException {
		new HandWritingNeuralNetENCOG();
	}
}