package uo.sdi.business.impl;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.jws.WebService;

import uo.sdi.business.LocalUserService;
import uo.sdi.business.RemoteUserService;
import uo.sdi.model.ListaApuntados;
import uo.sdi.model.User;
import uo.sdi.model.UserStatus;
import uo.sdi.persistence.PersistenceFactory;
import alb.util.log.Log;

@Stateless
@WebService(name = "UserService")
public class EJBUserService implements LocalUserService, RemoteUserService {

	public User buscaUsuario(String login) {
		return PersistenceFactory.newUserDao().findByLogin(login);
	}

	public void crearUsuario(User usuario, String comparaPass) throws Exception {
		usuario.setStatus(UserStatus.ACTIVE);
		try {
			if (comparaPass.equals(usuario.getPassword())) {
				PersistenceFactory.newUserDao().save(usuario);
				Log.info("El usuario [%s] ha sido creado satisfactoriamente",
						usuario.getLogin());
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			Log.error("Ha ocurrido algo creando al usuario [%s]",
					usuario.getLogin());
			throw new Exception();
		}
	}

	public void iniciaSesion(User usuario, Map<String, Object> session) {
		putUserInSession(usuario, session);
	}

	public void cerrarSesion(User usuario, Map<String, Object> session) {
		putUserOutSession(usuario, session);
	}

	public void actualizarUsario(User usuario) {
		PersistenceFactory.newUserDao().update(usuario);
	}

	private void putUserInSession(User user, Map<String, Object> session) {
		if (!user.getStatus().equals(UserStatus.CANCELLED))
			session.put("LOGGEDIN_USER", user);
	}

	private void putUserOutSession(User user, Map<String, Object> session) {
		;
		session.put("LOGGEDIN_USER", user);
	}

	@Override
	public List<User> getUsers() {
		return PersistenceFactory.newUserDao().findAll();
	}

	@Override
	public User findById(long idUsuario) {
		return PersistenceFactory.newUserDao().findById(idUsuario);
	}

	@Override
	public void updateUser(User u) {
		PersistenceFactory.newUserDao().update(u);
	}

	@Override
	public User buscaUsuarioPass(String login, String password) {
		return PersistenceFactory.newUserDao().findByLoginPassword(login,
				password);
	}

	@Override
	public boolean darDeBajaUsuario(Long id) {
		boolean existe = false;
		if (id != null && existeUsuario(id)) {
			existe = true;
			EJBApplicationService apS = new EJBApplicationService();
			User u = findById(id);
			u.setStatus(UserStatus.CANCELLED);
			List<ListaApuntados> user = apS.listaApuntadosUsuario(u);
			for (ListaApuntados l : user) {
				apS.cancelarUsuario(l);
				System.out.println("El usuario se ha cancelado con exito");
			}
			updateUser(u);
		}
		return existe;
	}
	
	private boolean existeUsuario(long id) {
		List<User> usuarios = getUsers();
		for (User u : usuarios) {
			if (u.getId() == id) {
				return true;
			}
		}
		return false;
	}
}
