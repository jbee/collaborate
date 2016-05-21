package vizio.io.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import vizio.Name;
import vizio.Product;
import vizio.io.Streamable;
import vizio.io.PersistenceManager;

/**
 * {@link Product} IO.
 *
 * <pre>
 * 1	length name => n
 * n	name byes
 * 4	tasks
 * 4	unconfirmedTasks
 * </pre>
 * @author jan
 *
 */
public class ProductStream implements Streamable<Product> {

	@Override
	public Product read(DataInputStream in, PersistenceManager pm) throws IOException {
		Product p = new Product();
		p.name = Streamable.readName(in);
		p.tasks = new AtomicInteger(in.readInt());
		p.unconfirmedTasks = new AtomicInteger(in.readInt());

		p.origin = pm.area(p.name, Name.ORIGIN);
		p.somewhere = pm.area(p.name, Name.UNKNOWN);
		p.somewhen = pm.version(p.name, Name.UNKNOWN);
		return p;
	}

	@Override
	public void write(Product p, DataOutputStream out) throws IOException {
		Streamable.writeName(p.name, out);
		out.writeInt(p.tasks.get());
		out.writeInt(p.unconfirmedTasks.get());
	}

}
