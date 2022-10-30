/*
 * Trabalho entregue à disciplina de Fundamentos Teóricos da Computação
 * Alunos: Rafael Murta e Rossana Souza
 *
 * O programa AfnParaAfd é um código em java que transforma autômatos finitos não determinísticos em determinísticos.
 *
 * Para rodar o programa é possível utilizar o executável ou compilar o código localmente.
 *
 * javac AfnParaAfd.java --> para compilar
 * java AfnParaAfd --> para executar
 *
 * O programa faz a leitura de arquivo xml com extensão .jff de nome afn.jff e lê o autômato de entrada que deve estar na mesma pasta que o executavel.
 * Em seguida lê um arquivo de extensão .txt de nome sentencas.txt com as sentenças que serão testadas naquela máquina que também deve estar na mesma pasta que o executavel.
 * Escreve um arquivo com extensão .jff de nome afd.jff com o autômato determinístico de saída.
 * E por último, escreve num arquvo .txt de nome resultado.txt que informa se as sentenças testadas são ou não reconhecidas pelo AF..
 *
 * O arquivo com as sentenças de entrada não deve possuir linhas vazias no final
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class AfnParaAfd {

	// Variáveis auxiliares
	static String sentenca = new String();
	static String[] vetorEstadosAfn;
	static String[] vetorEstadosAfd;
	static String[] vetorEstadosFinaisAfn;
	static String[] vetorEstadosFinaisAfd;

	static ArrayList<Transicao> listaTransicoesAfn = new ArrayList<Transicao>();
	static ArrayList<Transicao> listaTransicoesAfd = new ArrayList<Transicao>();

	static String estadoInicial = new String();
	static String[] alfabeto;

	static ArrayList<String> resultadoProcessamento = new ArrayList<String>();

	public static void main(String[] args) throws IOException {

		// Lê o arquivo que contêm o afn
		lehArquivoJff();

		// Lê várias sentenças do arquivo sentencas.txt
		FileReader leitorDeArquivo = new FileReader("sentencas.txt");
		BufferedReader bfr = new BufferedReader(leitorDeArquivo);

		while ((sentenca = bfr.readLine()) != null) {

			boolean erroDeCaractereInexistente = false;
			String caractereInexistente = "";

			/*
			 * Verifica se algum caractere da sentença de entrada não existe no alfabeto
			 * reconhecido pelo autômato
			 */
			String[] vetorDaSentenca = sentenca.split(",");
			for (String caractereDaSentenca : vetorDaSentenca) {
				if (!constaNoVetor(caractereDaSentenca, alfabeto)) {
					erroDeCaractereInexistente = true;
					caractereInexistente = caractereDaSentenca;
					break;
				}
			}
			if (erroDeCaractereInexistente) {
				System.err.println("Caractere não existe na linguagem: " + caractereInexistente);
			}

			/*
			 * Seta um novo estado inicial de acordo com o alcance das transições vazias
			 */
			ArrayList<String> listaDoEstadoInicialAfd = pegaTodasOsAlcancesEpsilon(estadoInicial,
					listaTransicoesAfn.toArray(new Transicao[listaTransicoesAfn.size()]));

			ArrayList<Transicao> listaAuxiliar = new ArrayList<>();

			ArrayList<ArrayList<String>> todosEstados = new ArrayList<>();

			/*
			 * Cria as transições
			 */
			listaAuxiliar = criaTransicoes(listaDoEstadoInicialAfd,
					listaTransicoesAfn.toArray(new Transicao[listaTransicoesAfn.size()]), alfabeto);

			for (int i = 0; i < listaAuxiliar.size(); i++) {
				adicionaAosEstados(todosEstados, listaAuxiliar.get(i).fromAll);
				adicionaAosEstados(todosEstados, listaAuxiliar.get(i).toAll);
				adicionaTransicaoSeNaoExistir(listaAuxiliar,
						criaTransicoes(listaAuxiliar.get(i).toAll,
								listaTransicoesAfn.toArray(new Transicao[listaTransicoesAfn.size()]), alfabeto));
			}

			// Setando todos os estados do AFD
			String stringEstadosAfd = "";
			for (int i = 0; i < todosEstados.size(); i++) {
				ArrayList<String> cadaEstado = todosEstados.get(i);
				stringEstadosAfd += adicionaEstado(cadaEstado);
				if (i < todosEstados.size() - 1) {
					stringEstadosAfd += ",";
				}
			}

			// Setando os estados finais do AFD
			String stringEstadosFinaisAfd = "";
			for (int i = 0; i < todosEstados.size(); i++) {
				ArrayList<String> estado = todosEstados.get(i);
				if (temAceitacao(vetorEstadosFinaisAfn, estado)) {
					stringEstadosFinaisAfd += adicionaEstado(estado);
					if (i < todosEstados.size() - 1) {
						stringEstadosFinaisAfd += ",";
					}
				}
			}

			/*
			 * Setando as transições do AFD --> cria uma string com todas as transições e
			 * transforma em lista posteriormente
			 */

			String transicoesAfd = "";
			for (int i = 0; i < listaAuxiliar.size(); i++) {
				transicoesAfd += adicionaEstado(listaAuxiliar.get(i).fromAll);
				transicoesAfd += ",";
				transicoesAfd += adicionaEstado(listaAuxiliar.get(i).toAll);
				transicoesAfd += ",";
				transicoesAfd += listaAuxiliar.get(i).letra;
				if (i < listaAuxiliar.size() - 1) {
					transicoesAfd += "#";
				}
			}

			/*
			 * Constroi o afd e verifica se as sentenças de entrada são reconhecidas por ele
			 */
			constroiAfdEProcessaEntrada(stringEstadosAfd, stringEstadosFinaisAfd, alfabeto, estadoInicial,
					transicoesAfd,
					sentenca);
		}
		bfr.close();

		/*
		 * Escreve no arquivo resultado.txt se cada sentença foi aceita ou rejeitada
		 * pelo AF
		 */
		BufferedWriter escreveSaida = new BufferedWriter(new FileWriter("resultado.txt"));
		resultadoProcessamento.stream().forEach(valor -> {
			try {
				escreveSaida.append(valor + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		escreveSaida.close();
	}

	public static void constroiAfdEProcessaEntrada(String estadosAfd, String estadosFinaisAfd, String[] alfabeto,
			String estadoInicial, String transicoesAfd, String sentenca) throws IOException {

		vetorEstadosAfd = estadosAfd.split(",");
		vetorEstadosFinaisAfd = estadosFinaisAfd.split(",");
		String[] vetorTransicoes;
		vetorTransicoes = transicoesAfd.split("#");
		boolean erroAlfabeto = false;

		for (String transicao : vetorTransicoes) {
			erroAlfabeto = false;
			String[] vetorAuxiliar = transicao.split(",");

			for (int i = 0; i < 2; i++) {
				if (!constaNoVetor(vetorAuxiliar[i], vetorEstadosAfd)) {
					erroAlfabeto = true;
					System.err.println("Transição inválida. " + vetorAuxiliar[i] +
							" não está incluído nos estados.");
					break;
				}
			}
			if (!constaNoVetor(vetorAuxiliar[2], alfabeto) &&
					!vetorAuxiliar[2].equals("$")) {
				erroAlfabeto = true;
				System.err.println("Transição inválida. " + vetorAuxiliar[2] +
						" não está incluído no alfabeto.");
			}
			if (erroAlfabeto) {
				break;
			}
			listaTransicoesAfd.add(new Transicao(vetorAuxiliar[0], vetorAuxiliar[1], vetorAuxiliar[2]));
		}

		boolean erroCaractereInvalido = false;
		String erroNaSentenca = "";
		String[] vetorSentenca = sentenca.split(",");
		for (String inputAlphabet : vetorSentenca) {
			if (!constaNoVetor(inputAlphabet, alfabeto)) {
				erroCaractereInvalido = true;
				erroNaSentenca = inputAlphabet;
				break;
			}
		}
		if (erroCaractereInvalido) {
			System.err.println("Caractere inválido: " + erroNaSentenca);
			return;
		}
		boolean erroTransicaoEstado = false;
		for (String estado : vetorEstadosAfd) {
			for (String letra : alfabeto) {
				if (!existeTransicao(estado, letra)) {
					erroTransicaoEstado = true;
					System.err.println("Falta transição para estado " + estado + " na entrada " + letra);
					break;
				}
			}
		}
		if (erroTransicaoEstado) {
			return;
		}

		escreveAfdJff("afd.jff", vetorEstadosAfd, estadoInicial, vetorEstadosFinaisAfd);

		String resultado = processaSentenca(sentenca);
		if (constaNoVetor(resultado, vetorEstadosFinaisAfd)) {
			resultadoProcessamento.add("Sentenca aceita: " + sentenca);
		} else {
			resultadoProcessamento.add("Sentenca rejeitada: " + sentenca);
		}

	}

	public static void escreveAfdJff(String path, String[] estados, String estadoInicial, String[] estadosFinais) {
		try {
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			Element root = document.createElement("structure");
			document.appendChild(root);
			// Elemento type
			Element type = document.createElement("type");
			type.appendChild(document.createTextNode("fa"));
			root.appendChild(type);
			// Elemento autômato
			Element automaton = document.createElement("automaton");
			root.appendChild(automaton);
			// Element estado
			for (int i = 0; i < estados.length; i++) {
				Element state = document.createElement("state");
				automaton.appendChild(state);

				state.setAttribute("id", estados[i].replaceAll("\\*", ""));
				if (estados[i].equals(estadoInicial)) {
					// Seta estado inicial
					Element initial = document.createElement("initial");
					state.appendChild(initial);
				}
				// Seta os estados finais
				for (int j = 0; j < estadosFinais.length; j++) {
					if (estados[i].equals(estadosFinais[j])) {
						Element finalState = document.createElement("final");
						state.appendChild(finalState);
					}
				}
				/*
				 * Seta os valores de x e y que são as coordenadas dos estados no JFLAP como
				 * (0,0) para todos os estados criados para o AFD
				 */
				Element x = document.createElement("x");
				x.appendChild(document.createTextNode("0.0"));
				state.appendChild(x);

				Element y = document.createElement("y");
				y.appendChild(document.createTextNode("0.0"));
				state.appendChild(y);

			}
			// Elemento transição
			for (int i = 0; i < listaTransicoesAfd.size(); i++) {
				Element transition = document.createElement("transition");
				automaton.appendChild(transition);
				// Estado de partida
				String de = listaTransicoesAfd.get(i).from.replaceAll("\\*", "");
				Element from = document.createElement("from");
				from.appendChild(document.createTextNode(de));
				transition.appendChild(from);
				// Estado de entrada
				String para = listaTransicoesAfd.get(i).to.replaceAll("\\*", "");
				Element to = document.createElement("to");
				to.appendChild(document.createTextNode(para));
				transition.appendChild(to);
				// Valor lido na transição
				Element read = document.createElement("read");
				read.appendChild(document.createTextNode((listaTransicoesAfd.get(i).letra)));
				transition.appendChild(read);
			}
			// Escreve o AFD no arquivo saida.jj
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(new File(path));
			transformer.transform(domSource, streamResult);
		} catch (Exception pce) {
			pce.printStackTrace();
		}

	}

	private static void adicionaAosEstados(ArrayList<ArrayList<String>> todosOsEstados, ArrayList<String> estado) {
		if (!todosOsEstados.contains(estado)) {
			todosOsEstados.add(estado);
		}
	}

	public static void adicionaTransicaoSeNaoExistir(ArrayList<Transicao> transicoes,
			ArrayList<Transicao> novasTransicoes) {
		for (int i = 0; i < novasTransicoes.size(); i++) {
			Collections.sort(novasTransicoes.get(i).fromAll);
			Collections.sort(novasTransicoes.get(i).toAll);
			int j;
			for (j = 0; j < transicoes.size(); j++) {
				Collections.sort(transicoes.get(j).fromAll);
				Collections.sort(transicoes.get(j).toAll);
				if (transicoes.get(j).fromAll.equals(novasTransicoes.get(i).fromAll)
						&& transicoes.get(j).toAll.equals(novasTransicoes.get(i).toAll)
						&& transicoes.get(j).letra.equals(novasTransicoes.get(i).letra)) {
					break;
				}
			}
			if (j == transicoes.size()) {
				transicoes.add(novasTransicoes.get(i));
			}
		}
	}

	private static String processaSentenca(String sentenca) {
		String estadoAtual = estadoInicial;
		String[] vetorSentenca = sentenca.split(",");
		for (int i = 0; i < vetorSentenca.length; i++) {
			for (int j = 0; j < listaTransicoesAfd.size(); j++) {
				if (listaTransicoesAfd.get(j).from.equals(estadoAtual)
						&& listaTransicoesAfd.get(j).letra.equals(vetorSentenca[i])) {
					estadoAtual = listaTransicoesAfd.get(j).to;
					break;
				}
			}
		}
		return estadoAtual;
	}

	private static boolean constaNoVetor(String caracter, String[] vetor) {
		for (int i = 0; i < vetor.length; i++) {
			if (vetor[i].equals(caracter)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<String> pegaAlcanceEpsilon(String estado, Transicao[] transicoes) {
		ArrayList<String> result = new ArrayList<>();
		result.add(estado);
		for (int i = 0; i < transicoes.length; i++) {
			if (transicoes[i].letra.equals("$") && transicoes[i].from.equals(estado)
					&& !result.contains(transicoes[i].to)) {
				result.add(transicoes[i].to);
			}
		}
		return result;
	}

	public static ArrayList<String> pegaTodasOsAlcancesEpsilon(String estado, Transicao[] transicoes) {
		ArrayList<String> result = pegaAlcanceEpsilon(estado, transicoes);
		for (int i = 0; i < result.size(); i++) {
			ArrayList<String> newOutcome = pegaAlcanceEpsilon(result.get(i), transicoes);
			for (int j = 0; j < newOutcome.size(); j++) {
				if (!result.contains(newOutcome.get(j))) {
					result.add(newOutcome.get(j));
				}
			}
		}
		return result;
	}

	public static boolean temAceitacao(String[] estadosDeAceitacao, ArrayList<String> estado) {
		for (int i = 0; i < estado.size(); i++) {
			for (int j = 0; j < estadosDeAceitacao.length; j++) {
				if (estadosDeAceitacao[j].equals(estado.get(i))) {
					return true;
				}
			}
		}
		return false;
	}

	public static ArrayList<String> pegaEstadoDeUmaEntrada(ArrayList<String> estado, Transicao[] transicoes,
			String letra) {
		ArrayList<String> resultado = new ArrayList<>();
		for (int i = 0; i < estado.size(); i++) {
			for (int j = 0; j < transicoes.length; j++) {
				if (transicoes[j].letra.equals(letra) && transicoes[j].from.equals(estado.get(i))
						&& !resultado.contains(transicoes[j].to)) {
					resultado.add(transicoes[j].to);
					adicionaSeAindaNaoContem(resultado, pegaTodasOsAlcancesEpsilon(transicoes[j].to, transicoes));
				}
			}
		}
		return resultado;
	}

	public static void adicionaSeAindaNaoContem(ArrayList<String> resultado, ArrayList<String> arrayToBeAdded) {
		for (int i = 0; i < arrayToBeAdded.size(); i++) {
			if (!resultado.contains(arrayToBeAdded.get(i))) {
				resultado.add(arrayToBeAdded.get(i));
			}
		}
	}

	public static ArrayList<Transicao> criaTransicoes(ArrayList<String> estado, Transicao[] transicoes,
			String[] alphabets) {
		ArrayList<Transicao> result = new ArrayList<>();
		for (int i = 0; i < alphabets.length; i++) {
			ArrayList<String> toStates = pegaEstadoDeUmaEntrada(estado, transicoes, alphabets[i]);
			if (toStates.size() == 0) {
				toStates.add("Dead");
			}
			result.add(new Transicao(estado, toStates, alphabets[i]));
		}
		return result;
	}

	public static String adicionaEstado(ArrayList<String> states) {
		String r = "";
		for (int i = 0; i < states.size(); i++) {
			r += states.get(i);
			if (i < states.size() - 1) {
				r += "*";
			}
		}
		return r;
	}

	private static boolean existeTransicao(String state, String letra) {
		for (int i = 0; i < listaTransicoesAfd.size(); i++) {
			if (listaTransicoesAfd.get(i).from.equals(state) && listaTransicoesAfd.get(i).letra.equals(letra)) {
				return true;
			}
		}
		return false;
	}

	public static void lehArquivoJff() {
		try {
			File inputFile = new File("afn.jff");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			XPath xPath = XPathFactory.newInstance().newXPath();

			/*
			 * Faz a leitura de todos os estados do autômato e armazena num vetor de estados
			 */
			String expression = "/structure/automaton/state";
			NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			ArrayList<String> finais = new ArrayList<String>();
			ArrayList<String> estadosAfn = new ArrayList<String>();
			for (int i = 0; i < nodeList.getLength(); i++) {

				Node nNode = nodeList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					estadosAfn.add(eElement.getAttribute("id"));

					if (eElement.getElementsByTagName("initial").getLength() > 0) {
						estadoInicial = eElement.getAttribute("id");
					}
					if (eElement.getElementsByTagName("final").getLength() > 0) {
						finais.add(eElement.getAttribute("id"));
					}
				}
			}
			vetorEstadosFinaisAfn = new String[finais.size()];
			for (int i = 0; i < finais.size(); i++) {
				vetorEstadosFinaisAfn[i] = finais.get(i);
			}
			vetorEstadosAfn = new String[estadosAfn.size()];
			for (int i = 0; i < estadosAfn.size(); i++) {
				vetorEstadosAfn[i] = estadosAfn.get(i);
			}

			/*
			 * Faz a leitura das transições do automato e armazena na lista de transições
			 */
			expression = "/structure/automaton/transition";
			nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
			ArrayList<String> caracteres = new ArrayList<String>();
			for (int i = 0; i < nodeList.getLength(); i++) {

				Node nNode = nodeList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					String caractere = new String();
					caractere = eElement.getElementsByTagName("read").item(0).getTextContent();

					listaTransicoesAfn
							.add((new Transicao(eElement.getElementsByTagName("from").item(0).getTextContent(),
									eElement.getElementsByTagName("to").item(0).getTextContent(), caractere)));

					if (!caracteres.contains(caractere)) {
						caracteres.add(caractere);
					}
				}
			}
			/*
			 * Seta todos os caracteres reconhecidos pelo autômato para o vetor alfabeto
			 */
			alfabeto = new String[caracteres.size()];
			for (int i = 0; i < caracteres.size(); i++) {
				alfabeto[i] = caracteres.get(i);
			}

		} catch (Exception e) {
			System.out.println(e);
		}
	}

}

/*
 * Usada para armazenar as transições do AF
 */
class Transicao {
	String from; // Estado de partida
	String to; // Estado destino
	ArrayList<String> fromAll; // Une todos os estados de partida para um mesmo caractere lido
	ArrayList<String> toAll; // Une todos os estados de destino para um mesmo caractere lido
	String letra; // Caractere lido de estado para o outro

	public Transicao(String from, String to, String letra) {
		this.from = from;
		this.to = to;
		this.letra = letra;
	}

	public Transicao(ArrayList<String> from, ArrayList<String> to, String letra) {
		this.fromAll = from;
		this.toAll = to;
		this.letra = letra;
	}
}