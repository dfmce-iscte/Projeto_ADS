package org.example;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {
    private final String file_caracterizacao_das_salas = "ADS - Caracterizacao das salas.csv";
    private final String file_horario_1sem = "2- ADS - Horários 1º sem 2022-23.csv";
    private final String file_avaliacoes_1sem = "ADS - Avaliações 1º semestre 2022-23.csv";
    private List<Sala> salas = new ArrayList<>();
    private List<Slot> slots = new ArrayList<>();

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
        //main.test();

    }

    private void test() {
        List<Sala> sala_to_return = new ArrayList<>();
        //sala_to_return = nearest_room(salas,130,sala_to_return);
        //System.out.println("SIZE: "+sala_to_return.size());
        sala_to_return = salas_match_caracteristica("Laboratório_de_Informática",salas);
        for (Sala sala: sala_to_return) {
            System.out.println(sala.toString());
        }

    }

    private void start() {
    	//readFile_caracterizacaoDasSalas();
    	readFile_slotsAula();
    	readFile_slotsAvaliacao();
    	
    }

    private void readFile_caracterizacaoDasSalas() {
        try {

            FileReader filereader = new FileReader(file_caracterizacao_das_salas);

            CSVReader csvReader = new CSVReader(filereader);
            String[] nextRecord;

            String[] columns = null;
            boolean first_line = true;

            // we are going to read data line by line
            while ((nextRecord = csvReader.readNext()) != null) {
                if (first_line) {
                	
                    columns = nextRecord[0].split(";");
                    first_line = false;
                } else {
                	
                    String[] line = nextRecord[0].split(";");
                    List<String> caracteristicas = new ArrayList<>();
                    

                    int num_caracteristicas = Integer.parseInt(line[4]);
                    for (int i = 5; i < line.length; i++) {
                        if (line[i].equalsIgnoreCase("x") && num_caracteristicas > 0) {
                            caracteristicas.add(columns[i]);
                            num_caracteristicas--;
                        }
                    }
                    Sala s = new Sala(line[0], line[1], Integer.parseInt(line[2]), Integer.parseInt(line[3]), caracteristicas);
                    salas.add(s);
                }
            }
           /*for (Sala s : salas) {
                System.out.println(s.toString());
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adicionar_espacos_a_eventos() {
        List<Slot> slots = new ArrayList<>();

        for (Slot slot : slots) {
            List<Evento> eventos_slot = slot.eventos;
            for (Evento e : eventos_slot) {
                if (e instanceof Avaliacao) check_salas_para_avaliacao((Avaliacao) e, slot);
                else if (e instanceof Aula) check_sala_para_aula((Aula) e,slot);
            }
        }
    }

    private List<Sala> nearest_room_for_evaluation(List<Sala> salas, int numero_alunos, List<Sala> sala_to_return) {
        int distance = salas.get(0).getCapacidade_exame() - numero_alunos;
        int index=0;

        for (int i=1; i < salas.size(); i++) {
            int temp_distance = salas.get(i).getCapacidade_exame() - numero_alunos;
            if ((temp_distance>=0 && temp_distance < distance) ||
                    distance < 0 && temp_distance > distance) {
                index=i;
                distance=temp_distance;
            }
        }
        Sala sala_index = salas.get(index);
        if (sala_index.getCapacidade_exame() >= numero_alunos) sala_to_return.add(sala_index);
        else {
            int new_numero_alunos = numero_alunos - sala_index.getCapacidade_exame();
            salas.remove(index);
            sala_to_return.add(sala_index);
            sala_to_return = nearest_room_for_evaluation(salas,new_numero_alunos,sala_to_return);
        }
        return sala_to_return;
    }

    private void check_salas_para_avaliacao(Avaliacao a, Slot slot) {
        List<Sala> salas_livres = slot.salas_livres;
        String estado_pedido = a.getEstado_pedido_sala();
        if (estado_pedido.equals("Aberto") || estado_pedido.equals("Novo")) {
            List<Sala> sala_to_return = new ArrayList<>();
            sala_to_return = nearest_room_for_evaluation(salas_livres,a.getNumero_de_alunos(),sala_to_return);
            if (!sala_to_return.isEmpty()) {
                a.setSalas(sala_to_return);
                int capacidade_salas=0;
                for (Sala s:sala_to_return) {
                    capacidade_salas+=s.getCapacidade_exame();
                }
                a.setCapacidade_salas(capacidade_salas);
            } else {
                System.err.println("Não encontrou nenhuma aula para a avaliação");
            }
        }
    }

    private List<Sala> salas_match_caracteristica(String caracteristica, List<Sala> salas_livres) {
        List<Sala> salas = new ArrayList<>();
        for (Sala s : salas_livres) {
            if (s.getCaracteristicas().contains(caracteristica)) salas.add(s);
        }
        return salas;
    }

    private Sala check_sala_para_aula(Aula aula, Slot slot) {
        List<Sala> salas_livres = slot.salas_livres;
        String caracteristica_aula = aula.caracteristica;
        List<Sala> salas_livres_com_caracteristica = salas_match_caracteristica(caracteristica_aula, salas_livres);

        if (!salas_livres_com_caracteristica.isEmpty()) {
            int diferenca = 0; //diferenca entre capacidade da sala e numero de inscritos
            Sala sala_to_return = null;

            for (Sala s : salas_livres_com_caracteristica) {
                int temp_dif = s.getCapacidade_normal() - aula.turno.inscritos;
                if (temp_dif >= 0 && temp_dif < diferenca) {
                    diferenca = temp_dif;
                    sala_to_return = s;
                }
            }
            if (sala_to_return != null) return sala_to_return;
            else {
                //Aqui há salas com aquela caracteristica disponiveis mas com capacidade inferior
                System.out.println("Sobrelotação");
                resolver_conflito();
            }
        }
        //Aqui nao ha salas com aquela caracteristica
        resolver_conflito();
        return null;
    }

    private void resolver_conflito() {

    }
    
    
	private void fillSlot(Evento evento, String data, String hora_inicio, String hora_final) {
		List<Evento> eventos = new ArrayList<>();
		eventos.add(evento);
		Slot new_slot = new Slot(data, hora_inicio, hora_final, eventos, salas);
		boolean duplicate = false;

		for (Slot s : slots) {
			if (s.data.equals(new_slot.data) && s.hora_inicio.equals(new_slot.hora_inicio)
					&& s.hora_final.equals(new_slot.hora_final)) {
				s.eventos.add(evento);
				duplicate = true;
			}
		}

		if (!duplicate) {
			slots.add(new_slot);
		}
	}
    
    private void readFile_slotsAula() {
		
		try {
			
			FileReader filereader = new FileReader(file_horario_1sem);

			try (CSVReader csvReader = new CSVReader(filereader)) {
				String[] nextRecord;
				List<Sala> salas_livres = salas;
				
				boolean first_line = true;
				while ((nextRecord = csvReader.readNext()) != null) {
					if (first_line) {
						first_line = false;
						
					} else {
						String[] line = nextRecord;
						if(line[0].isEmpty()) {
							break;
						}
						
						String unidade_de_execucao = line[1];
						String[] cursos = line[0].split(","); 
						Evento evento = null;
						
						
						String hora_inicial = line[8];
						String hora_final = line[9];

						Date date = null;
						if(!line[10].isEmpty()) {
						date = new SimpleDateFormat("dd-MM-yyyy").parse(line[10]);}
						
						
						evento = new Aula(date, date, Integer.parseInt(line[4]), cursos, unidade_de_execucao,
								hora_inicial, hora_final);
					
						fillSlot(evento, line[10], line[8], line[9]);
					}
				}
			}
			//for (Slot s: slots) {
				//System.out.println(s.toString());
			//}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    
    
    private void readFile_slotsAvaliacao() {
		
		try {
			
			FileReader filereader = new FileReader(file_avaliacoes_1sem);

			try (CSVReader csvReader = new CSVReader(filereader)) {
				String[] nextRecord;
				List<Sala> salas_livres = salas;
				
				boolean first_line = true;
				while ((nextRecord = csvReader.readNext()) != null) {
					if (first_line) {
						first_line = false;
						
					} else {
						String[] line = nextRecord;
						if(line[0].isEmpty()) {
							break;
						}
						
						String unidade_de_execucao = line[1];
						String[] cursos = line[0].split(","); 
						Evento evento = null;
						
							String[] data = line[8].split(" ");
							
							String hora_inicial = data[1];
							String hora_final = null;
							Date data_inicial = null;
							Date data_final = null;
							data_inicial = new SimpleDateFormat("yyyy/MM/dd").parse(data[0]);
							
							
							
							if(data.length==5) {
							hora_final = data[4];
							data_final = new SimpleDateFormat("yyyy/MM/dd").parse(data[3]);
							}
							else {
								hora_final = data[3];
								data_final = data_inicial;
							}
							
							
							evento = new Avaliacao(data_inicial, data_final, Integer.parseInt(line[12]), cursos, unidade_de_execucao,
									hora_inicial, hora_final, line);
						
						
						
							fillSlot(evento, data[0], hora_inicial, hora_final);
					}
				}
			}
			//for (Slot s: slots) {
				//System.out.println(s.toString());
			//}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}